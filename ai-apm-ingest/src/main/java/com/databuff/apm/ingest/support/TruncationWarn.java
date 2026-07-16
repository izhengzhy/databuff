package com.databuff.apm.ingest.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate-limited WARN when ingest truncates oversized log {@code body} fields.
 * First truncation logs immediately; later ones are summarized at most once per interval.
 */
public final class TruncationWarn {

    private static final Logger log = LoggerFactory.getLogger(TruncationWarn.class);
    private static final long INTERVAL_MS = 10_000L;

    private static final AtomicLong lastEmitMs = new AtomicLong(0);
    private static final AtomicLong pending = new AtomicLong(0);

    private TruncationWarn() {
    }

    /** @return {@code true} if a WARN was emitted for this call */
    public static boolean logBodyTruncated(String service, int originalLength, int maxLength) {
        pending.incrementAndGet();
        long now = System.currentTimeMillis();
        long last = lastEmitMs.get();
        if (last != 0 && now - last < INTERVAL_MS) {
            return false;
        }
        if (!lastEmitMs.compareAndSet(last, now)) {
            return false;
        }
        long count = pending.getAndSet(0);
        log.warn(
                "log body truncated {} time(s) since last warn; latest from {} to {} chars, service={}",
                count,
                originalLength,
                maxLength,
                service == null || service.isBlank() ? "?" : service);
        return true;
    }

    /** Visible for tests. */
    static void resetForTest() {
        lastEmitMs.set(0);
        pending.set(0);
    }

    /** Visible for tests: pretend the last warn was {@code agoMs} ago. */
    static void setLastEmitAgoForTest(long agoMs) {
        lastEmitMs.set(System.currentTimeMillis() - agoMs);
    }
}
