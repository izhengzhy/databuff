package com.databuff.apm.ingest.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TruncationWarnTest {

    @BeforeEach
    void reset() {
        TruncationWarn.resetForTest();
    }

    @Test
    void firstTruncationWarnsImmediatelyThenRateLimits() {
        assertThat(TruncationWarn.logBodyTruncated("checkout", 200_000, 1000)).isTrue();
        assertThat(TruncationWarn.logBodyTruncated("checkout", 200_001, 1000)).isFalse();
        assertThat(TruncationWarn.logBodyTruncated("other", 50_000, 1000)).isFalse();

        TruncationWarn.setLastEmitAgoForTest(11_000L);
        assertThat(TruncationWarn.logBodyTruncated("payments", 9_000, 1000)).isTrue();
    }
}
