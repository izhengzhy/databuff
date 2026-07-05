package com.databuff.apm.web.tools.local;

import com.databuff.apm.common.time.ApmTimeZones;
import com.databuff.apm.web.portal.LogPortalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local APM log query tools for the data and inspection experts.
 */
@Component
@Lazy
public class LogTools {

    private static final int DEFAULT_LOG_SIZE = 50;
    private static final int MAX_LOG_SIZE = 200;
    private static final long DEFAULT_TRACE_LOOKBACK_MS = 86_400_000L;

    @Autowired
    private LogPortalService logPortalService;
    @Autowired
    private ObjectMapper objectMapper;

    @Tool(converter = PlainTextToolResultConverter.class, description = "Query log volume trend over time. Filter by service, service instance, severity, or body keyword. Requires fromTime/toTime in yyyy-MM-dd HH:mm:ss. Do not use for log line details or pagination. Do not use hostname as service instance.")
    public String queryLogTrend(
            @ToolParam(name = "fromTime", description = "Required start time, format yyyy-MM-dd HH:mm:ss")
            String fromTime,
            @ToolParam(name = "toTime", description = "Required end time, format yyyy-MM-dd HH:mm:ss")
            String toTime,
            @ToolParam(name = "services", required = false, description = "Optional service names")
            List<String> services,
            @ToolParam(name = "serviceIds", required = false, description = "Optional service IDs")
            List<String> serviceIds,
            @ToolParam(name = "serviceInstances", required = false, description = "Optional service instance IDs (OTel service.instance.id); not hostname")
            List<String> serviceInstances,
            @ToolParam(name = "severities", required = false, description = "Optional severity filters such as ERROR, WARN, INFO")
            List<String> severities,
            @ToolParam(name = "query", required = false, description = "Optional body keyword")
            String query,
            @ToolParam(name = "interval", required = false, description = "Optional bucket interval in seconds, default 60")
            Integer interval) {
        String timeRangeError = validateTimeRange(fromTime, toTime);
        if (timeRangeError != null) {
            return error(timeRangeError);
        }
        Map<String, Object> body = filterBody(fromTime, toTime, services, serviceIds, serviceInstances, severities, query);
        body.put("interval", interval == null ? 60 : Math.max(1, interval));
        Map<String, Object> response = logPortalService.trend(body);
        return json(wrapTrend(response, fromTime, toTime, interval));
    }

    @Tool(converter = PlainTextToolResultConverter.class, description = "Query paginated log detail lines. Filter by service, service instance, severity, or body keyword. Requires fromTime/toTime. Do not pass traceId or spanId; use queryLogsByTraceId or queryLogsBySpanId instead. serviceInstances maps to OTel service.instance.id, not hostname.")
    public String queryLogDetail(
            @ToolParam(name = "fromTime", description = "Required start time, format yyyy-MM-dd HH:mm:ss")
            String fromTime,
            @ToolParam(name = "toTime", description = "Required end time, format yyyy-MM-dd HH:mm:ss")
            String toTime,
            @ToolParam(name = "services", required = false, description = "Optional service names")
            List<String> services,
            @ToolParam(name = "serviceIds", required = false, description = "Optional service IDs")
            List<String> serviceIds,
            @ToolParam(name = "serviceInstances", required = false, description = "Optional service instance IDs (OTel service.instance.id)")
            List<String> serviceInstances,
            @ToolParam(name = "severities", required = false, description = "Optional severity filters such as ERROR, WARN, INFO")
            List<String> severities,
            @ToolParam(name = "query", required = false, description = "Optional body keyword")
            String query,
            @ToolParam(name = "offset", required = false, description = "Optional pagination offset, default 0")
            Integer offset,
            @ToolParam(name = "size", required = false, description = "Optional page size, default 50, max 200")
            Integer size) {
        String timeRangeError = validateTimeRange(fromTime, toTime);
        if (timeRangeError != null) {
            return error(timeRangeError);
        }
        return searchResponse(
                filterBody(fromTime, toTime, services, serviceIds, serviceInstances, severities, query),
                offset,
                size,
                fromTime,
                toTime);
    }

    @Tool(converter = PlainTextToolResultConverter.class, description = "Query paginated log lines for one traceId. Optional severity/body filters. Defaults to the last 24 hours when fromTime/toTime are omitted.")
    public String queryLogsByTraceId(
            @ToolParam(name = "traceId", description = "Required trace ID")
            String traceId,
            @ToolParam(name = "severities", required = false, description = "Optional severity filters")
            List<String> severities,
            @ToolParam(name = "query", required = false, description = "Optional body keyword")
            String query,
            @ToolParam(name = "fromTime", required = false, description = "Optional start time, defaults to last 24 hours")
            String fromTime,
            @ToolParam(name = "toTime", required = false, description = "Optional end time, defaults to now")
            String toTime,
            @ToolParam(name = "offset", required = false, description = "Optional pagination offset, default 0")
            Integer offset,
            @ToolParam(name = "size", required = false, description = "Optional page size, default 50, max 200")
            Integer size) {
        if (isBlank(traceId)) {
            return error("traceId is required");
        }
        TimeWindow window;
        try {
            window = resolveTimeWindow(fromTime, toTime, DEFAULT_TRACE_LOOKBACK_MS);
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fromTime", window.fromTime());
        body.put("toTime", window.toTime());
        body.put("traceId", traceId.trim());
        putStringList(body, "severities", severities);
        putIfNotBlank(body, "query", query);
        return searchResponse(body, offset, size, window.fromTime(), window.toTime());
    }

    @Tool(converter = PlainTextToolResultConverter.class, description = "Query paginated log lines for one spanId. Pass traceId when known to avoid collisions. Defaults to the last 24 hours when fromTime/toTime are omitted.")
    public String queryLogsBySpanId(
            @ToolParam(name = "spanId", description = "Required span ID")
            String spanId,
            @ToolParam(name = "traceId", required = false, description = "Optional but recommended trace ID")
            String traceId,
            @ToolParam(name = "severities", required = false, description = "Optional severity filters")
            List<String> severities,
            @ToolParam(name = "query", required = false, description = "Optional body keyword")
            String query,
            @ToolParam(name = "fromTime", required = false, description = "Optional start time, defaults to last 24 hours")
            String fromTime,
            @ToolParam(name = "toTime", required = false, description = "Optional end time, defaults to now")
            String toTime,
            @ToolParam(name = "offset", required = false, description = "Optional pagination offset, default 0")
            Integer offset,
            @ToolParam(name = "size", required = false, description = "Optional page size, default 50, max 200")
            Integer size) {
        if (isBlank(spanId)) {
            return error("spanId is required");
        }
        TimeWindow window;
        try {
            window = resolveTimeWindow(fromTime, toTime, DEFAULT_TRACE_LOOKBACK_MS);
        } catch (IllegalArgumentException ex) {
            return error(ex.getMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fromTime", window.fromTime());
        body.put("toTime", window.toTime());
        body.put("spanId", spanId.trim());
        putIfNotBlank(body, "traceId", traceId);
        putStringList(body, "severities", severities);
        putIfNotBlank(body, "query", query);
        return searchResponse(body, offset, size, window.fromTime(), window.toTime());
    }

    private String searchResponse(
            Map<String, Object> body,
            Integer offset,
            Integer size,
            String fromTime,
            String toTime) {
        int resolvedOffset = offset == null ? 0 : Math.max(0, offset);
        int resolvedSize = Math.max(1, Math.min(size == null ? DEFAULT_LOG_SIZE : size, MAX_LOG_SIZE));
        body.put("offset", resolvedOffset);
        body.put("size", resolvedSize);
        Map<String, Object> response = logPortalService.search(body);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromTime", ApmTimeZones.normalizeWallClockText(fromTime));
        result.put("toTime", ApmTimeZones.normalizeWallClockText(toTime));
        result.put("total", response.getOrDefault("total", 0L));
        result.put("offset", response.getOrDefault("offset", resolvedOffset));
        result.put("size", resolvedSize);
        result.put("data", response.getOrDefault("data", List.of()));
        return json(result);
    }

    private static Map<String, Object> filterBody(
            String fromTime,
            String toTime,
            List<String> services,
            List<String> serviceIds,
            List<String> serviceInstances,
            List<String> severities,
            String query) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fromTime", ApmTimeZones.normalizeWallClockText(fromTime));
        body.put("toTime", ApmTimeZones.normalizeWallClockText(toTime));
        putStringList(body, "services", services);
        putStringList(body, "serviceIds", serviceIds);
        putStringList(body, "serviceInstances", serviceInstances);
        putStringList(body, "severities", severities);
        putIfNotBlank(body, "query", query);
        return body;
    }

    private static Map<String, Object> wrapTrend(
            Map<String, Object> response,
            String fromTime,
            String toTime,
            Integer interval) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromTime", ApmTimeZones.normalizeWallClockText(fromTime));
        result.put("toTime", ApmTimeZones.normalizeWallClockText(toTime));
        result.put("interval", interval == null ? 60 : Math.max(1, interval));
        result.put("data", response.getOrDefault("data", Map.of()));
        return result;
    }

    private static TimeWindow resolveTimeWindow(String fromTime, String toTime, long defaultLookbackMs) {
        if (isBlank(fromTime) && isBlank(toTime)) {
            long now = System.currentTimeMillis();
            return new TimeWindow(
                    ApmTimeZones.WALL_CLOCK.format(Instant.ofEpochMilli(now - defaultLookbackMs)),
                    ApmTimeZones.WALL_CLOCK.format(Instant.ofEpochMilli(now)));
        }
        String timeRangeError = validateTimeRange(fromTime, toTime);
        if (timeRangeError != null) {
            throw new IllegalArgumentException(timeRangeError);
        }
        return new TimeWindow(
                ApmTimeZones.normalizeWallClockText(fromTime),
                ApmTimeZones.normalizeWallClockText(toTime));
    }

    private static void putStringList(Map<String, Object> body, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            if (!isBlank(value)) {
                cleaned.add(value.trim());
            }
        }
        if (!cleaned.isEmpty()) {
            body.put(key, cleaned);
        }
    }

    private static void putIfNotBlank(Map<String, Object> body, String key, String value) {
        if (!isBlank(value)) {
            body.put(key, value.trim());
        }
    }

    private static String validateTimeRange(String fromTime, String toTime) {
        if (isBlank(fromTime) && isBlank(toTime)) {
            return "fromTime and toTime are required in yyyy-MM-dd HH:mm:ss; call getCurrentTimeRange or getTimeRangeAroundTime first";
        }
        if (isBlank(fromTime)) {
            return "fromTime is required in yyyy-MM-dd HH:mm:ss";
        }
        if (isBlank(toTime)) {
            return "toTime is required in yyyy-MM-dd HH:mm:ss";
        }
        return null;
    }

    private static boolean isBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String normalized = value.trim();
        return "null".equalsIgnoreCase(normalized) || "undefined".equalsIgnoreCase(normalized);
    }

    private String error(String message) {
        return json(Map.of("ok", false, "message", message));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private record TimeWindow(String fromTime, String toTime) {
    }
}
