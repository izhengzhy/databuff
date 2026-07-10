package com.databuff.apm.ingest.support;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Limits repetitive warn/error logs: callers record each suppressed event, and at most one
 * summary is emitted per {@link #intervalMs}.
 */
public final class LogRateLimiter {

    private final long intervalMs;
    private final AtomicLong pending = new AtomicLong();
    private volatile long lastEmitMs;

    public LogRateLimiter(long intervalMs) {
        this.intervalMs = Math.max(1_000L, intervalMs);
        this.lastEmitMs = System.currentTimeMillis();
    }

    /**
     * Records one suppressed event.
     *
     * @return event count to include in a summary log, or {@code 0} if still within the interval
     */
    public long record() {
        pending.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - lastEmitMs < intervalMs) {
            return 0;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - lastEmitMs < intervalMs) {
                return 0;
            }
            lastEmitMs = now;
            return pending.getAndSet(0);
        }
    }
}
