package com.databuff.apm.ingest.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogRateLimiterTest {

    @Test
    void emitsAtMostOncePerInterval() throws InterruptedException {
        LogRateLimiter limiter = new LogRateLimiter(1_000L);

        assertThat(limiter.record()).isZero();
        assertThat(limiter.record()).isZero();

        Thread.sleep(1_050);

        assertThat(limiter.record()).isEqualTo(3);
        assertThat(limiter.record()).isZero();
    }
}
