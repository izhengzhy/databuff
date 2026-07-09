package com.databuff.apm.common.storage;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetricRowTimeUtilTest {

    @Test
    void putsTsAndShanghaiWallClockMetricTime() {
        Map<String, Object> row = new HashMap<>();
        MetricRowTimeUtil.putTsAndMetricTime(row, 1_704_067_200_000L);
        assertThat(row.get("ts")).isEqualTo(1_704_067_200_000L);
        assertThat(row.get("metric_time")).isEqualTo("2024-01-01 08:00:00");
    }
}
