package com.databuff.apm.ingest.trace;

import com.databuff.apm.common.model.DcSpan;
import com.databuff.apm.common.trace.TraceParentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Step 3 · 按 traceId 聚合 span（内存缓冲 + 定时检测）。
 * <p>
 * 分布式场景下同一 trace 的 span 会分多批到达；在 flush 前尽量等待收齐。
 * 检测周期默认 2s，满足以下任一条件即 flush：
 * <ul>
 *   <li>root span 已到，且连续 2 次检测无新 span</li>
 *   <li>连续 4 次检测无新 span（无论 root 是否已到）</li>
 * </ul>
 */
public final class TraceAssemblyBuffer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TraceAssemblyBuffer.class);

    static final int IDLE_CHECKS_WITH_ROOT = 2;
    static final int IDLE_CHECKS_WITHOUT_ROOT = 4;

    private final long checkIntervalMs;
    private final Map<String, TraceBucket> pending = new LinkedHashMap<>();
    private final BiConsumer<String, List<DcSpan>> flushHandler;
    private final ScheduledExecutorService scheduler;
    private final String schedulerName;

    public TraceAssemblyBuffer(long checkIntervalMs, BiConsumer<String, List<DcSpan>> flushHandler) {
        this(checkIntervalMs, flushHandler, "trace-assembly");
    }

    public TraceAssemblyBuffer(long checkIntervalMs, BiConsumer<String, List<DcSpan>> flushHandler, String schedulerName) {
        this.checkIntervalMs = Math.max(100L, checkIntervalMs);
        this.flushHandler = Objects.requireNonNull(flushHandler);
        this.schedulerName = schedulerName == null || schedulerName.isBlank() ? "trace-assembly" : schedulerName;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, this.schedulerName);
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler.scheduleAtFixedRate(
                this::runCheckSafe,
                this.checkIntervalMs,
                this.checkIntervalMs,
                TimeUnit.MILLISECONDS);
        log.info(
                "Trace assembly buffer started checkIntervalMs={} idleWithRoot={} idleWithoutRoot={}",
                this.checkIntervalMs,
                IDLE_CHECKS_WITH_ROOT,
                IDLE_CHECKS_WITHOUT_ROOT);
    }

    /**
     * 归入 traceId 桶；无 traceId 时立即 flush。
     *
     * @return 需要立即 flush 的 span 列表（仅无 traceId 时非空）
     */
    public List<DcSpan> offer(DcSpan span) {
        Objects.requireNonNull(span, "span");
        String traceId = span.trace_id;
        if (traceId == null || traceId.isBlank()) {
            return List.of(span);
        }
        synchronized (this) {
            TraceBucket bucket = pending.computeIfAbsent(traceId, ignored -> new TraceBucket());
            bucket.add(span);
        }
        return List.of();
    }

    /** 批量归入同一 traceId；无 traceId 的 span 立即 flush。 */
    public List<DcSpan> offerAll(List<DcSpan> spans) {
        if (spans == null || spans.isEmpty()) {
            return List.of();
        }
        List<DcSpan> immediate = new ArrayList<>();
        synchronized (this) {
            for (DcSpan span : spans) {
                if (span == null) {
                    continue;
                }
                String traceId = span.trace_id;
                if (traceId == null || traceId.isBlank()) {
                    immediate.add(span);
                    continue;
                }
                TraceBucket bucket = pending.computeIfAbsent(traceId, ignored -> new TraceBucket());
                bucket.add(span);
            }
        }
        return immediate;
    }

    /** 关闭或超时场景：刷出所有 pending trace。 */
    public synchronized List<List<DcSpan>> flushAll() {
        List<List<DcSpan>> all = new ArrayList<>();
        for (Map.Entry<String, TraceBucket> entry : pending.entrySet()) {
            all.add(entry.getValue().copySpans());
        }
        pending.clear();
        return all;
    }

    public synchronized int pendingTraces() {
        return pending.size();
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }

    private void runCheckSafe() {
        try {
            runCheck();
        } catch (Exception e) {
            log.warn("Trace assembly check failed: {}", e.getMessage(), e);
        }
    }

    private void runCheck() {
        List<ReadyTrace> ready = new ArrayList<>();
        synchronized (this) {
            for (Map.Entry<String, TraceBucket> entry : pending.entrySet()) {
                String traceId = entry.getKey();
                TraceBucket bucket = entry.getValue();
                if (!bucket.seenOnLastCheck) {
                    bucket.seenOnLastCheck = true;
                    bucket.spanCountAtLastCheck = bucket.spans.size();
                    continue;
                }
                int currentCount = bucket.spans.size();
                if (currentCount == bucket.spanCountAtLastCheck) {
                    bucket.idleChecks++;
                } else {
                    bucket.idleChecks = 0;
                    bucket.spanCountAtLastCheck = currentCount;
                }
                if (shouldFlush(bucket)) {
                    ready.add(new ReadyTrace(traceId, bucket.copySpans(), bucket.spans.size(), bucket.rootSpanSeen, bucket.idleChecks));
                }
            }
            for (ReadyTrace batch : ready) {
                pending.remove(batch.traceId);
            }
        }
        for (ReadyTrace batch : ready) {
            flushHandler.accept(batch.traceId, batch.spans);
        }
    }

    private static boolean shouldFlush(TraceBucket bucket) {
        if (bucket.rootSpanSeen && bucket.idleChecks >= IDLE_CHECKS_WITH_ROOT) {
            return true;
        }
        return bucket.idleChecks >= IDLE_CHECKS_WITHOUT_ROOT;
    }

    static boolean isRootSpan(DcSpan span) {
        return span != null && TraceParentUtil.isRootParentId(span.parent_id);
    }

    private static final class TraceBucket {
        private final List<DcSpan> spans = new ArrayList<>();
        private boolean rootSpanSeen;
        private boolean seenOnLastCheck;
        private int spanCountAtLastCheck;
        private int idleChecks;

        void add(DcSpan span) {
            spans.add(span);
            if (isRootSpan(span)) {
                rootSpanSeen = true;
            }
            idleChecks = 0;
        }

        List<DcSpan> copySpans() {
            return List.copyOf(spans);
        }
    }

    private record ReadyTrace(String traceId, List<DcSpan> spans, int spanCount, boolean rootSpanSeen, int idleChecks) {
    }
}
