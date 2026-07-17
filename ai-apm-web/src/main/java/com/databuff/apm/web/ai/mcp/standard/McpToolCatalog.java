package com.databuff.apm.web.ai.mcp.standard;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class McpToolCatalog {

    private final List<McpToolDefinition> tools;

    public McpToolCatalog() {
        this.tools = List.of(
                tool("getCurrentTimeRange",
                        "Get current query time range",
                        schema(Map.of("rangeMinutes", integerProp("Minutes to look back from now")))),
                tool("getTimeRangeAroundTime",
                        "Get query time range around a HH:mm target time",
                        schema(Map.of("targetTime", stringProp("Target time in HH:mm format")))),
                tool("drawTrendCharts",
                        "Draw multiple trend charts from queried metric data",
                        schema(Map.of("charts", arrayProp("Trend chart specifications")))),
                tool("queryServicesAll",
                        "Query service list from service catalog; optional fromTime/toTime for time-windowed list",
                        schema(Map.of(
                                "keyword", stringProp("Optional service name keyword filter"),
                                "fromTime", stringProp("Query start time"),
                                "toTime", stringProp("Query end time")))),
                tool("queryServicesByServiceType",
                        "Query service list by serviceType from service catalog; optional fromTime/toTime",
                        schema(Map.of(
                                "serviceType", stringProp("Service type filter"),
                                "keyword", stringProp("Optional service name keyword filter"),
                                "size", integerProp("Maximum number of results"),
                                "fromTime", stringProp("Query start time"),
                                "toTime", stringProp("Query end time")))),
                tool("queryServiceTopology",
                        "Query upstream and downstream topology for one service by service name",
                        schema(Map.of(
                                "serviceName", stringProp("Service name"),
                                "serviceInstance", stringProp("Optional service instance"),
                                "fromTime", stringProp("Query start time"),
                                "toTime", stringProp("Query end time")),
                        List.of("serviceName"))),
                tool("queryTraceListByCondition",
                        "Query trace list by service call condition",
                        schema(Map.of(
                                "srcServiceId", stringProp("Source service ID"),
                                "serviceId", stringProp("Target service ID"),
                                "componentType", stringProp("Component type filter"),
                                "resource", stringProp("Resource filter"),
                                "direction", stringProp("Call direction"),
                                "fromTime", stringProp("Query start time"),
                                "toTime", stringProp("Query end time"),
                                "size", integerProp("Maximum number of results")))),
                tool("queryTraceDetail",
                        "Query trace detail by traceId",
                        schema(Map.of("traceId", stringProp("Trace ID")),
                        List.of("traceId"))),
                tool("queryServiceAlarms",
                        "Query alarm data for one service entity",
                        schema(Map.of(
                                "serviceId", stringProp("Service ID"),
                                "status", integerProp("Alarm status filter"),
                                "fromTime", stringProp("Query start time"),
                                "toTime", stringProp("Query end time")))),
                tool("queryMetricData",
                        "Query Doris metric tables by metric_core measurement, field, and tags",
                        schema(Map.of(
                                "queryRequests", arrayProp("Metric query request list"),
                                "size", integerProp("Maximum number of results per query")))),
                tool("queryLogTrend",
                        "Query log volume trend by service, service instance, severity, or keyword",
                        schema(logTrendSchema(), List.of("fromTime", "toTime"))),
                tool("queryLogDetail",
                        "Query paginated log detail lines; do not pass traceId or spanId",
                        schema(logDetailSchema(), List.of("fromTime", "toTime"))),
                tool("queryLogsByTraceId",
                        "Query paginated log lines for one traceId",
                        schema(Map.of(
                                "traceId", stringProp("Trace ID"),
                                "severities", stringArrayProp("Optional severity filters"),
                                "query", stringProp("Optional body keyword"),
                                "fromTime", stringProp("Optional start time, defaults to last 24 hours"),
                                "toTime", stringProp("Optional end time, defaults to now"),
                                "offset", integerProp("Pagination offset"),
                                "size", integerProp("Page size, max 200")),
                        List.of("traceId"))),
                tool("queryLogsBySpanId",
                        "Query paginated log lines for one spanId; pass traceId when known",
                        schema(Map.of(
                                "spanId", stringProp("Span ID"),
                                "traceId", stringProp("Optional but recommended trace ID"),
                                "severities", stringArrayProp("Optional severity filters"),
                                "query", stringProp("Optional body keyword"),
                                "fromTime", stringProp("Optional start time, defaults to last 24 hours"),
                                "toTime", stringProp("Optional end time, defaults to now"),
                                "offset", integerProp("Pagination offset"),
                                "size", integerProp("Page size, max 200")),
                        List.of("spanId"))),
                tool("inspectService",
                        "Inspect one service: entry metrics, ERROR/WARN/keyword logs, alarms, dependencies, error traces, instances; web also checks exception/JVM/CPU/memory",
                        schema(Map.of("serviceName", stringProp("Service name to inspect")),
                        List.of("serviceName"))));
    }

    public List<McpToolDefinition> listTools() {
        return tools;
    }

    public Optional<McpToolDefinition> findByName(String name) {
        return tools.stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    private static McpToolDefinition tool(String name, String description, Map<String, Object> inputSchema) {
        return new McpToolDefinition(name, description, inputSchema, implementationFor(name));
    }

    private static Map<String, Object> logTrendSchema() {
        Map<String, Object> props = new LinkedHashMap<>(logFilterSchema());
        props.put("interval", integerProp("Optional bucket interval in seconds, default 60"));
        return props;
    }

    private static Map<String, Object> logDetailSchema() {
        Map<String, Object> props = new LinkedHashMap<>(logFilterSchema());
        props.put("offset", integerProp("Pagination offset, default 0"));
        props.put("size", integerProp("Page size, default 50, max 200"));
        return props;
    }

    private static Map<String, Object> logFilterSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("fromTime", stringProp("Query start time in yyyy-MM-dd HH:mm:ss"));
        props.put("toTime", stringProp("Query end time in yyyy-MM-dd HH:mm:ss"));
        props.put("services", stringArrayProp("Optional service names"));
        props.put("serviceIds", stringArrayProp("Optional service IDs"));
        props.put("serviceInstances", stringArrayProp("Optional service instance IDs (OTel service.instance.id)"));
        props.put("severities", stringArrayProp("Optional severity filters such as ERROR, WARN, INFO"));
        props.put("query", stringProp("Optional body keyword"));
        return props;
    }

    private static String implementationFor(String name) {
        return switch (name) {
            case "getCurrentTimeRange" -> "commonTools.getCurrentTimeRange";
            case "getTimeRangeAroundTime" -> "commonTools.getTimeRangeAroundTime";
            case "drawTrendCharts" -> "commonTools.drawTrendCharts";
            case "queryServicesAll" -> "dataTools.queryServicesAll";
            case "queryServicesByServiceType" -> "dataTools.queryServicesByServiceType";
            case "queryServiceTopology" -> "dataTools.queryServiceTopology";
            case "queryTraceListByCondition" -> "dataTools.queryTraceListByCondition";
            case "queryTraceDetail" -> "dataTools.queryTraceDetail";
            case "queryServiceAlarms" -> "dataTools.queryServiceAlarms";
            case "queryMetricData" -> "dataTools.queryMetricData";
            case "queryLogTrend" -> "logTools.queryLogTrend";
            case "queryLogDetail" -> "logTools.queryLogDetail";
            case "queryLogsByTraceId" -> "logTools.queryLogsByTraceId";
            case "queryLogsBySpanId" -> "logTools.queryLogsBySpanId";
            case "inspectService" -> "inspectTools.inspectService";
            default -> throw new IllegalArgumentException("unknown MCP tool: " + name);
        };
    }

    private static Map<String, Object> schema(Map<String, Object> properties) {
        return schema(properties, List.of());
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private static Map<String, Object> stringProp(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static Map<String, Object> integerProp(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private static Map<String, Object> arrayProp(String description) {
        return Map.of("type", "array", "description", description);
    }

    private static Map<String, Object> stringArrayProp(String description) {
        return Map.of(
                "type", "array",
                "description", description,
                "items", Map.of("type", "string"));
    }

    public record McpToolDefinition(
            String name,
            String description,
            Map<String, Object> inputSchema,
            String implementation) {
    }
}
