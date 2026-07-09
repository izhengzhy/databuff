package com.databuff.apm.common.storage;

import com.databuff.apm.common.time.ApmTimeZones;

import java.util.Map;

/** Populates Doris metric row time columns ({@code ts} millis + {@code metric_time} wall clock). */
public final class MetricRowTimeUtil {

    private MetricRowTimeUtil() {
    }

    public static void putTsAndMetricTime(Map<String, Object> row, long tsMillis) {
        row.put("ts", tsMillis);
        row.put("metric_time", ApmTimeZones.formatWallClock(tsMillis));
    }
}
