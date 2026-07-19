package com.databuff.apm.web.tools.local;

import com.databuff.apm.common.time.ApmTimeZones;
import com.databuff.apm.common.storage.ApmReadRepository;
import com.databuff.apm.common.storage.MetricQueryBuilder;
import com.databuff.apm.common.util.PortalServiceIdResolver;
import com.databuff.apm.web.config.ApmStorageProperties;
import com.databuff.apm.web.monitor.Alarm;
import com.databuff.apm.web.monitor.AlarmStore;
import com.databuff.apm.web.monitor.service.AlarmService;
import com.databuff.apm.web.portal.LogPortalService;
import com.databuff.apm.web.portal.ServicePortalService;
import com.databuff.apm.web.portal.TracePortalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
@Lazy
public class InspectTools {

    private static final int ALARM_SAMPLE_SIZE = 10;
    private static final int LOG_SAMPLE_SIZE = 5;
    private static final int TRACE_SAMPLE_SIZE = 8;
    private static final int DEPENDENCY_SAMPLE_SIZE = 8;
    private static final int INSPECTION_CONCURRENCY = 8;
    private static final int INSPECTION_QUEUE_CAPACITY = 64;
    private static final double DEPENDENCY_ERR_RATE_THRESHOLD = 0.05;
    private static final Set<String> ERROR_SEVERITIES = Set.of("ERROR", "FATAL", "error", "fatal");
    private static final Set<String> WARN_SEVERITIES = Set.of("WARN", "WARNING", "warn", "warning");
    private static final List<String> LOG_KEYWORDS = List.of(
            "OutOfMemory", "OOM", "timeout", "Timed out", "Connection refused", "Deadlock", "NullPointerException");

    @Autowired
    private ServicePortalService servicePortalService;
    @Autowired
    private LogPortalService logPortalService;
    @Autowired
    private TracePortalService tracePortalService;
    @Autowired
    private AlarmService alarmService;
    @Autowired
    private AlarmStore alarmStore;
    @Autowired
    private ApmReadRepository readRepository;
    @Autowired
    private ApmStorageProperties storageProperties;
    @Autowired
    private ObjectMapper objectMapper;
    private final ExecutorService inspectionExecutor = createInspectionExecutor();
    private String metricDatabase;

    @PostConstruct
    void initMetricDatabase() {
        metricDatabase = storageProperties == null ? "databuff" : storageProperties.metricDatabase();
    }

    @PreDestroy
    void shutdownInspectionExecutor() {
        inspectionExecutor.shutdown();
    }

    @Tool(converter = PlainTextToolResultConverter.class, description = "Inspect one service by serviceName. No time range input is required. Covers entry metrics, ERROR/WARN logs, keyword logs, alarms, dependencies, error traces, instances; web services also check exception, JVM GC, CPU and memory.")
    public String inspectService(
            @ToolParam(name = "serviceName", description = "Service name to inspect")
            String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return json(Map.of("ok", false, "message", "serviceName is required"));
        }
        String service = serviceName.trim();
        long nowMillis = System.currentTimeMillis();
        String from = ApmTimeZones.WALL_CLOCK.format(Instant.ofEpochMilli(nowMillis - 3_600_000L));
        String to = ApmTimeZones.WALL_CLOCK.format(Instant.ofEpochMilli(nowMillis));
        String prevFrom = ApmTimeZones.WALL_CLOCK.format(Instant.ofEpochMilli(nowMillis - 7_200_000L));
        String prevTo = from;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("serviceId", service);
        body.put("serviceName", service);
        body.put("fromTime", from);
        body.put("toTime", to);
        body.put("interval", 60);
        body.put("size", 50);

        Map<String, Object> serviceInfo = servicePortalService.serviceInfo(body);
        String serviceType = stringValue(serviceInfo == null ? null : serviceInfo.get("service_type"), "");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("serviceName", service);
        result.put("serviceInfo", serviceInfo == null ? Map.of() : serviceInfo);
        result.put("fromTime", from);
        result.put("toTime", to);
        CompletableFuture<Map<String, Object>> entryMetrics =
                submitInspection(() -> inspectEntryMetrics(body));
        CompletableFuture<Map<String, Object>> logMetrics =
                submitInspection(() -> inspectLogMetrics(service, from, to));
        List<CompletableFuture<KeywordInspection>> keywordInspections = LOG_KEYWORDS.stream()
                .map(keyword -> submitInspection(() -> inspectLogKeyword(service, from, to, keyword)))
                .toList();
        CompletableFuture<Map<String, Object>> alarmMetrics =
                submitInspection(() -> inspectAlarmMetrics(service, serviceInfo, from, to));
        CompletableFuture<Map<String, Object>> dependencyMetrics =
                submitInspection(() -> inspectDependencyMetrics(service, from, to));
        CompletableFuture<Map<String, Object>> traceMetrics =
                submitInspection(() -> inspectErrorTraces(service, from, to));
        CompletableFuture<Map<String, Object>> instanceMetrics =
                submitInspection(() -> inspectInstanceMetrics(
                        service, from, to, prevFrom, prevTo, serviceInfo));

        boolean webService = isWebService(serviceType, serviceInfo);
        CompletableFuture<Map<String, Object>> exceptionMetrics = webService
                ? submitInspection(() -> inspectExceptionMetrics(body))
                : null;
        CompletableFuture<Map<String, Object>> jvmMetrics = webService
                ? submitInspection(() -> inspectJvmMetrics(service, from, to))
                : null;
        CompletableFuture<Map<String, Object>> resourceMetrics = webService
                ? submitInspection(() -> inspectResourceMetrics(service, from, to))
                : null;

        result.put("entryMetrics", await(entryMetrics));
        result.put("logMetrics", await(logMetrics));
        result.put("logKeywordMetrics", inspectLogKeywords(keywordInspections));
        result.put("alarmMetrics", await(alarmMetrics));
        result.put("dependencyMetrics", await(dependencyMetrics));
        result.put("traceMetrics", await(traceMetrics));
        result.put("instanceMetrics", await(instanceMetrics));

        if (webService) {
            result.put("exceptionMetrics", await(exceptionMetrics));
            result.put("jvmMetrics", await(jvmMetrics));
            result.put("resourceMetrics", await(resourceMetrics));
        }
        result.put("summary", summarize(result));
        return json(result);
    }

    private Map<String, Object> inspectEntryMetrics(Map<String, Object> baseBody) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reqCount", inspectTrendMetric(baseBody, "reqCount", "入口请求量"));
        result.put("errRate", inspectTrendMetric(baseBody, "errRate", "入口错误率"));
        result.put("avgTime", inspectTrendMetric(baseBody, "avgTime", "入口平均响应时间"));
        return result;
    }

    private Map<String, Object> inspectTrendMetric(Map<String, Object> baseBody, String metric, String label) {
        Map<String, Object> body = new LinkedHashMap<>(baseBody);
        body.put("metric", metric);
        List<Map<String, Object>> series = servicePortalService.serviceDetailTrendChart(body);
        List<Double> values = extractSeriesValues(series);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("metric", metric);
        result.put("label", label);
        result.put("data", series);
        result.put("detection", detectAnomaly(values));
        return result;
    }

    private Map<String, Object> inspectExceptionMetrics(Map<String, Object> baseBody) {
        Map<String, Object> body = new LinkedHashMap<>(baseBody);
        body.put("groupBy", "exceptionName");
        body.put("size", 10);
        Map<String, Object> data = servicePortalService.exceptionDistMap(body);
        List<Double> values = new ArrayList<>();
        Object list = data == null ? null : data.get("list");
        if (list instanceof Iterable<?> rows) {
            for (Object row : rows) {
                if (row instanceof Map<?, ?> map) {
                    values.add(numberValue(map.get("errCnt")));
                }
            }
        }
        return Map.of(
                "label", "服务异常分布",
                "data", data == null ? Map.of() : data,
                "detection", detectAnomaly(values));
    }

    private Map<String, Object> inspectLogMetrics(String serviceName, String fromTime, String toTime) {
        Map<String, Object> result = new LinkedHashMap<>();
        Object severityCnts = queryLogSeverityTrend(serviceName, fromTime, toTime);
        result.put("errorLogs", inspectLogSeverityTrend(severityCnts, ERROR_SEVERITIES, "ERROR 日志量"));
        result.put("warnLogs", inspectLogSeverityTrend(severityCnts, WARN_SEVERITIES, "WARN 日志量"));
        result.put("errorSamples", sampleErrorLogs(serviceName, fromTime, toTime));
        return result;
    }

    private Object queryLogSeverityTrend(
            String serviceName,
            String fromTime,
            String toTime) {
        if (logPortalService == null) {
            return null;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fromTime", fromTime);
            body.put("toTime", toTime);
            body.put("services", List.of(serviceName));
            body.put("interval", 60);
            Map<String, Object> response = logPortalService.trend(body);
            Object dataNode = response == null ? null : response.get("data");
            Map<?, ?> data = dataNode instanceof Map<?, ?> map ? map : Map.of();
            return data.get("severityCnts");
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> inspectLogSeverityTrend(
            Object severityCnts,
            Set<String> severities,
            String label) {
        List<Double> values = extractSeveritySeries(severityCnts, severities);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", label);
        result.put("data", Map.of(
                "severityCnts", severityCnts == null ? Map.of() : severityCnts,
                "total", values.stream().mapToDouble(Double::doubleValue).sum()));
        result.put("detection", detectAnomaly(values));
        if (severityCnts == null) {
            result.put("error", logPortalService == null
                    ? "logPortalService is not ready"
                    : "log trend query failed");
        }
        return result;
    }

    private List<Map<String, Object>> sampleErrorLogs(String serviceName, String fromTime, String toTime) {
        if (logPortalService == null) {
            return List.of();
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fromTime", fromTime);
            body.put("toTime", toTime);
            body.put("services", List.of(serviceName));
            body.put("severities", List.of("ERROR", "FATAL"));
            body.put("offset", 0);
            body.put("size", LOG_SAMPLE_SIZE);
            Map<String, Object> response = logPortalService.search(body);
            Object data = response == null ? null : response.get("data");
            if (!(data instanceof List<?> rows)) {
                return List.of();
            }
            List<Map<String, Object>> samples = new ArrayList<>();
            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> map)) {
                    continue;
                }
                Map<String, Object> sample = new LinkedHashMap<>();
                sample.put("timestamp", map.get("timestamp"));
                sample.put("severity", map.get("severity"));
                sample.put("message", map.get("message") != null ? map.get("message") : map.get("body"));
                sample.put("traceId", map.get("traceId"));
                samples.add(sample);
                if (samples.size() >= LOG_SAMPLE_SIZE) {
                    break;
                }
            }
            return samples;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Map<String, Object> inspectAlarmMetrics(
            String serviceName,
            Map<String, Object> serviceInfo,
            String fromTime,
            String toTime) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", "服务告警");
        List<String> aliases = serviceAliases(serviceName, serviceInfo);
        long recentTotal = 0L;
        List<Map<String, Object>> samples = new ArrayList<>();
        if (alarmService != null) {
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("serviceId", serviceName);
                body.put("fromTime", fromTime);
                body.put("toTime", toTime);
                body.put("offset", 0);
                body.put("size", ALARM_SAMPLE_SIZE);
                body.put("pageSize", ALARM_SAMPLE_SIZE);
                body.put("pageNum", 1);
                Map<String, Object> response = alarmService.list(body);
                Object dataNode = response == null ? null : response.get("data");
                if (dataNode instanceof Map<?, ?> data) {
                    recentTotal = (long) numberValue(data.get("total"));
                    Object list = data.get("list");
                    if (list instanceof Iterable<?> rows) {
                        for (Object row : rows) {
                            if (!(row instanceof Map<?, ?> map)) {
                                continue;
                            }
                            Map<String, Object> sample = new LinkedHashMap<>();
                            sample.put("id", map.get("id"));
                            sample.put("description", map.get("description"));
                            sample.put("level", map.get("level"));
                            sample.put("ruleName", map.get("ruleName"));
                            sample.put("startTriggerTime", map.get("startTriggerTime"));
                            samples.add(sample);
                            if (samples.size() >= ALARM_SAMPLE_SIZE) {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                result.put("error", e.getMessage() == null ? "alarm query failed" : e.getMessage());
            }
        }
        long openCount = 0L;
        List<Map<String, Object>> openSamples = new ArrayList<>();
        if (alarmStore != null) {
            for (Alarm alarm : alarmStore.listOpen()) {
                if (!matchesAnyAlias(alarm.service(), aliases)) {
                    continue;
                }
                openCount++;
                if (openSamples.size() < ALARM_SAMPLE_SIZE) {
                    Map<String, Object> sample = new LinkedHashMap<>();
                    sample.put("id", alarm.id());
                    sample.put("description", alarm.message());
                    sample.put("level", alarm.level());
                    sample.put("status", alarm.status());
                    sample.put("triggeredAt", alarm.triggeredAt() == null ? null : alarm.triggeredAt().toEpochMilli());
                    openSamples.add(sample);
                }
            }
        }
        result.put("recentTotal", recentTotal);
        result.put("openCount", openCount);
        result.put("recentSamples", samples);
        result.put("openSamples", openSamples);
        boolean anomaly = openCount > 0 || recentTotal > 0;
        Map<String, Object> detection = new LinkedHashMap<>();
        detection.put("anomaly", anomaly);
        detection.put("openCount", openCount);
        detection.put("recentTotal", recentTotal);
        detection.put("reason", anomaly
                ? (openCount > 0
                ? "存在未恢复告警 " + openCount + " 条"
                : "近1小时内触发告警 " + recentTotal + " 条")
                : "未发现告警");
        result.put("detection", detection);
        return result;
    }

    private Map<String, Object> inspectJvmMetrics(String serviceName, String fromTime, String toTime) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (readRepository == null) {
            result.put("available", false);
            result.put("message", "readRepository is not ready");
            return result;
        }
        long fromMillis = ApmTimeZones.wallClockToEpochMilli(fromTime);
        long toMillis = ApmTimeZones.wallClockToEpochMilli(toTime);
        result.put("gcMajorCount", inspectJvmSqlMetric(
                serviceName, fromMillis, toMillis, "metric_jvm", "gc_major_collection_count", "JVM Major GC 次数"));
        result.put("gcMajorTime", inspectJvmSqlMetric(
                serviceName, fromMillis, toMillis, "metric_jvm", "gc_major_collection_time", "JVM Major GC 耗时"));
        result.put("threadCount", inspectJvmSqlMetric(
                serviceName, fromMillis, toMillis, "metric_jvm", "thread_count", "JVM 线程数"));
        return result;
    }

    private Map<String, Object> inspectResourceMetrics(String serviceName, String fromTime, String toTime) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (readRepository == null) {
            result.put("available", false);
            result.put("message", "readRepository is not ready");
            return result;
        }
        long fromMillis = ApmTimeZones.wallClockToEpochMilli(fromTime);
        long toMillis = ApmTimeZones.wallClockToEpochMilli(toTime);
        result.put("cpuUsage", inspectJvmSqlMetric(
                serviceName, fromMillis, toMillis, "metric_service_cpu", "usage_pct", "服务 CPU 使用率"));
        result.put("memUsage", inspectJvmSqlMetric(
                serviceName, fromMillis, toMillis, "metric_service_mem", "usage_pct", "服务内存使用率"));
        return result;
    }

    private Map<String, Object> inspectLogKeywords(
            List<CompletableFuture<KeywordInspection>> keywordInspections) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", "日志关键词");
        List<Map<String, Object>> hits = new ArrayList<>();
        List<Map<String, Object>> samples = new ArrayList<>();
        long totalHits = 0L;
        for (CompletableFuture<KeywordInspection> future : keywordInspections) {
            KeywordInspection inspection = await(future);
            if (inspection.total() <= 0) {
                continue;
            }
            totalHits += inspection.total();
            hits.add(Map.of("keyword", inspection.keyword(), "total", inspection.total()));
            inspection.samples().stream()
                    .limit(Math.max(0, LOG_SAMPLE_SIZE - samples.size()))
                    .forEach(samples::add);
        }
        result.put("hits", hits);
        result.put("totalHits", totalHits);
        result.put("samples", samples);
        Map<String, Object> detection = new LinkedHashMap<>();
        detection.put("anomaly", totalHits > 0);
        detection.put("totalHits", totalHits);
        detection.put("reason", totalHits > 0
                ? "命中异常关键词日志 " + totalHits + " 条"
                : "未命中 OOM/timeout 等关键词");
        result.put("detection", detection);
        return result;
    }

    private KeywordInspection inspectLogKeyword(
            String serviceName,
            String fromTime,
            String toTime,
            String keyword) {
        if (logPortalService == null) {
            return new KeywordInspection(keyword, 0L, List.of());
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("fromTime", fromTime);
            body.put("toTime", toTime);
            body.put("services", List.of(serviceName));
            body.put("query", keyword);
            body.put("offset", 0);
            body.put("size", 3);
            Map<String, Object> response = logPortalService.search(body);
            long total = response == null ? 0L : (long) numberValue(response.get("total"));
            if (total <= 0) {
                return new KeywordInspection(keyword, 0L, List.of());
            }
            List<Map<String, Object>> samples = new ArrayList<>();
            Object data = response.get("data");
            if (data instanceof List<?> rows) {
                for (Object row : rows) {
                    if (!(row instanceof Map<?, ?> map)) {
                        continue;
                    }
                    Map<String, Object> sample = new LinkedHashMap<>();
                    sample.put("keyword", keyword);
                    sample.put("timestamp", map.get("timestamp"));
                    sample.put("severity", map.get("severity"));
                    sample.put("message", map.get("message") != null ? map.get("message") : map.get("body"));
                    sample.put("traceId", map.get("traceId"));
                    samples.add(sample);
                }
            }
            return new KeywordInspection(keyword, total, samples);
        } catch (Exception ignored) {
            return new KeywordInspection(keyword, 0L, List.of());
        }
    }

    private Map<String, Object> inspectDependencyMetrics(String serviceName, String fromTime, String toTime) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", "上下游依赖");
        List<Map<String, Object>> suspicious = new ArrayList<>();
        List<Map<String, Object>> downstream = new ArrayList<>();
        List<Map<String, Object>> upstream = new ArrayList<>();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("serviceId", serviceName);
            body.put("serviceName", serviceName);
            body.put("fromTime", fromTime);
            body.put("toTime", toTime);
            Map<String, Object> topology = servicePortalService.getServiceInstanceRelations(body);
            Map<String, String> nameById = peerNameIndex(topology == null ? null : topology.get("serviceId2Name"));
            downstream = summarizePeers(topology == null ? null : topology.get("downflowServiceStats"), nameById, "downstream");
            upstream = summarizePeers(topology == null ? null : topology.get("upflowServiceStats"), nameById, "upstream");
            for (Map<String, Object> peer : downstream) {
                if (Boolean.TRUE.equals(peer.get("suspicious"))) {
                    suspicious.add(peer);
                }
            }
            for (Map<String, Object> peer : upstream) {
                if (Boolean.TRUE.equals(peer.get("suspicious")) && suspicious.size() < DEPENDENCY_SAMPLE_SIZE) {
                    suspicious.add(peer);
                }
            }
        } catch (Exception e) {
            result.put("error", e.getMessage() == null ? "dependency query failed" : e.getMessage());
        }
        result.put("downstream", downstream.stream().limit(DEPENDENCY_SAMPLE_SIZE).toList());
        result.put("upstream", upstream.stream().limit(DEPENDENCY_SAMPLE_SIZE).toList());
        result.put("suspicious", suspicious.stream().limit(DEPENDENCY_SAMPLE_SIZE).toList());
        Map<String, Object> detection = new LinkedHashMap<>();
        detection.put("anomaly", !suspicious.isEmpty());
        detection.put("suspiciousCount", suspicious.size());
        detection.put("reason", suspicious.isEmpty()
                ? "上下游依赖未发现明显错误放大"
                : "发现可疑依赖 " + suspicious.size() + " 个（错误率偏高）");
        result.put("detection", detection);
        return result;
    }

    private Map<String, Object> inspectErrorTraces(String serviceName, String fromTime, String toTime) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", "失败 Trace");
        long total = 0L;
        List<Map<String, Object>> samples = new ArrayList<>();
        if (tracePortalService != null) {
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("serviceId", serviceName);
                body.put("serviceName", serviceName);
                body.put("fromTime", fromTime);
                body.put("toTime", toTime);
                body.put("offset", 0);
                body.put("size", TRACE_SAMPLE_SIZE);
                body.put("error", 1);
                Map<String, Object> response = tracePortalService.errorSpanList(body);
                Object dataNode = response == null ? null : response.get("data");
                if (dataNode instanceof Map<?, ?> data) {
                    total = (long) numberValue(data.get("total"));
                    Object list = data.get("list");
                    if (list instanceof Iterable<?> rows) {
                        for (Object row : rows) {
                            if (!(row instanceof Map<?, ?> map)) {
                                continue;
                            }
                            Map<String, Object> sample = new LinkedHashMap<>();
                            sample.put("traceId", map.get("traceId"));
                            sample.put("spanId", map.get("spanId"));
                            sample.put("resource", firstNonBlank(
                                    stringValue(map.get("resource"), ""),
                                    stringValue(map.get("operationName"), "")));
                            sample.put("errorType", map.get("errorType"));
                            sample.put("startTime", map.get("startTime"));
                            sample.put("duration", map.get("duration"));
                            samples.add(sample);
                            if (samples.size() >= TRACE_SAMPLE_SIZE) {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                result.put("error", e.getMessage() == null ? "error trace query failed" : e.getMessage());
            }
        }
        result.put("total", total);
        result.put("samples", samples);
        Map<String, Object> detection = new LinkedHashMap<>();
        detection.put("anomaly", total > 0);
        detection.put("total", total);
        detection.put("reason", total > 0
                ? "近1小时失败 Trace " + total + " 条"
                : "未发现失败 Trace");
        result.put("detection", detection);
        return result;
    }

    private Map<String, Object> inspectInstanceMetrics(
            String serviceName,
            String fromTime,
            String toTime,
            String prevFrom,
            String prevTo,
            Map<String, Object> serviceInfo) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("label", "服务实例");
        List<Map<String, Object>> current = listInstances(serviceName, fromTime, toTime, serviceInfo);
        List<Map<String, Object>> previous = listInstances(serviceName, prevFrom, prevTo, serviceInfo);
        int currentCount = current.size();
        int previousCount = previous.size();
        Set<String> previousIds = new java.util.HashSet<>();
        for (Map<String, Object> row : previous) {
            previousIds.add(stringValue(row.get("serviceInstance"), ""));
        }
        List<String> disappeared = new ArrayList<>();
        Set<String> currentIds = new java.util.HashSet<>();
        for (Map<String, Object> row : current) {
            String id = stringValue(row.get("serviceInstance"), "");
            currentIds.add(id);
        }
        for (String id : previousIds) {
            if (!id.isBlank() && !currentIds.contains(id)) {
                disappeared.add(id);
            }
        }
        boolean dropped = previousCount >= 2 && currentCount * 2 < previousCount;
        boolean vanished = previousCount > 0 && currentCount == 0;
        boolean anomaly = vanished || dropped || !disappeared.isEmpty();
        result.put("currentCount", currentCount);
        result.put("previousCount", previousCount);
        result.put("instances", current.stream().limit(20).toList());
        result.put("disappeared", disappeared.stream().limit(20).toList());
        Map<String, Object> detection = new LinkedHashMap<>();
        detection.put("anomaly", anomaly);
        detection.put("currentCount", currentCount);
        detection.put("previousCount", previousCount);
        detection.put("disappearedCount", disappeared.size());
        String reason;
        if (vanished) {
            reason = "当前窗口无活跃实例，上一窗口有 " + previousCount + " 个";
        } else if (dropped) {
            reason = "实例数明显下降：" + previousCount + " → " + currentCount;
        } else if (!disappeared.isEmpty()) {
            reason = "有实例消失：" + disappeared.size() + " 个";
        } else {
            reason = "实例数稳定（当前 " + currentCount + "）";
        }
        detection.put("reason", reason);
        result.put("detection", detection);
        return result;
    }

    private List<Map<String, Object>> listInstances(
            String serviceName,
            String fromTime,
            String toTime,
            Map<String, Object> serviceInfo) {
        if (servicePortalService == null) {
            return List.of();
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("serviceId", serviceName);
            body.put("serviceName", serviceName);
            body.put("fromTime", fromTime);
            body.put("toTime", toTime);
            List<Map<String, Object>> rows = servicePortalService.getServiceInstance(body, serviceInfo);
            if (rows == null) {
                return List.of();
            }
            List<Map<String, Object>> compact = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("serviceInstance", row.get("serviceInstance"));
                item.put("hostName", row.get("hostName"));
                item.put("hostIp", row.get("hostIp"));
                item.put("serviceCall", row.get("serviceCall"));
                compact.add(item);
            }
            return compact;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static Map<String, String> peerNameIndex(Object serviceId2Name) {
        Map<String, String> names = new LinkedHashMap<>();
        if (!(serviceId2Name instanceof Iterable<?> rows)) {
            return names;
        }
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            String id = stringValue(map.get("serviceId"), "");
            String name = firstNonBlank(stringValue(map.get("name"), ""), stringValue(map.get("service"), ""));
            if (!id.isBlank() && !name.isBlank()) {
                names.put(id, name);
            }
        }
        return names;
    }

    private static List<Map<String, Object>> summarizePeers(
            Object peers,
            Map<String, String> nameById,
            String direction) {
        if (!(peers instanceof Iterable<?> rows)) {
            return List.of();
        }
        List<Map<String, Object>> summarized = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            long outCnt = (long) numberValueStatic(map.get("reqOutCnt"));
            long outErr = (long) numberValueStatic(map.get("reqOutErrCnt"));
            long inCnt = (long) numberValueStatic(map.get("reqInCnt"));
            long inErr = (long) numberValueStatic(map.get("reqInErrCnt"));
            long callCnt = Math.max(outCnt, inCnt);
            long errCnt = Math.max(outErr, inErr);
            if (callCnt <= 0 && errCnt <= 0) {
                continue;
            }
            double errRate = callCnt > 0 ? (double) errCnt / callCnt : 0;
            String serviceId = stringValue(map.get("serviceId"), "");
            Map<String, Object> peer = new LinkedHashMap<>();
            peer.put("direction", direction);
            peer.put("serviceId", serviceId);
            peer.put("service", nameById.getOrDefault(serviceId, serviceId));
            peer.put("componentType", map.get("componentType"));
            peer.put("callCnt", callCnt);
            peer.put("errCnt", errCnt);
            peer.put("errRate", errRate);
            boolean suspicious = (callCnt >= 5 && errRate >= DEPENDENCY_ERR_RATE_THRESHOLD) || errCnt >= 20;
            peer.put("suspicious", suspicious);
            summarized.add(peer);
        }
        summarized.sort((left, right) -> Double.compare(
                numberValueStatic(right.get("errRate")),
                numberValueStatic(left.get("errRate"))));
        return summarized;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private Map<String, Object> inspectJvmSqlMetric(
            String serviceName,
            long fromMillis,
            long toMillis,
            String table,
            String field,
            String label) {
        String filter = " AND `service` = '" + escapeLiteral(serviceName) + "'";
        String sql = MetricQueryBuilder.isJvmGcMonotonicField(field)
                ? MetricQueryBuilder.metricFieldSeriesSql(
                        metricDatabase, table, field, fromMillis, toMillis, filter, 60, "sum")
                : """
                SELECT CAST(FLOOR(`ts` / 60000) * 60 AS BIGINT) AS epoch_sec, SUM(`%s`) AS metric_value
                FROM %s.`%s`
                WHERE `ts` >= %d AND `ts` < %d AND `service` = '%s'
                GROUP BY epoch_sec
                ORDER BY epoch_sec ASC
                LIMIT 120
                """.formatted(field, metricDatabase, table, fromMillis, toMillis, escapeLiteral(serviceName));
        try {
            List<Map<String, Object>> rows = MetricToolResultFormat.formatEpochSecRows(readRepository.queryRows(sql, 120));
            List<Double> values = rows.stream()
                    .map(row -> numberValue(row.get("metric_value")))
                    .toList();
            return Map.of("label", label, "data", rows, "detection", detectAnomaly(values));
        } catch (Exception e) {
            return Map.of("label", label, "data", List.of(), "detection", detectAnomaly(List.of()),
                    "error", e.getMessage() == null ? "query failed" : e.getMessage());
        }
    }

    public static Map<String, Object> detectAnomaly(List<Double> values) {
        List<Double> normalized = values == null ? List.of() : values.stream()
                .filter(value -> value != null && Double.isFinite(value))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("points", normalized.size());
        if (normalized.size() < 3) {
            result.put("anomaly", false);
            result.put("reason", "数据点不足");
            return result;
        }
        double mean = normalized.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = normalized.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0);
        double std = Math.sqrt(variance);
        double latest = normalized.get(normalized.size() - 1);
        double max = normalized.stream().mapToDouble(Double::doubleValue).max().orElse(latest);
        boolean spike = std > 0 && latest > mean + 3 * std;
        boolean jump = mean > 0 && latest > mean * 2 && latest == max;
        boolean anomaly = spike || jump;
        result.put("anomaly", anomaly);
        result.put("latest", latest);
        result.put("mean", mean);
        result.put("std", std);
        result.put("max", max);
        result.put("reason", anomaly ? "最新点相对历史均值存在明显突增" : "未发现明显突增");
        return result;
    }

    private List<Double> extractSeriesValues(List<Map<String, Object>> series) {
        if (series == null || series.isEmpty()) {
            return List.of();
        }
        List<Double> values = new ArrayList<>();
        for (Map<String, Object> item : series) {
            Object rawValues = item.get("values");
            if (!(rawValues instanceof Iterable<?> points)) {
                continue;
            }
            for (Object point : points) {
                if (point instanceof List<?> tuple && tuple.size() >= 2) {
                    values.add(numberValue(tuple.get(1)));
                }
            }
        }
        return values;
    }

    private static String summarize(Map<String, Object> result) {
        List<String> abnormal = new ArrayList<>();
        collectAbnormalLabels(result.get("entryMetrics"), abnormal);
        collectAbnormalLabels(result.get("logMetrics"), abnormal);
        collectAbnormalLabels(result.get("logKeywordMetrics"), abnormal);
        collectAbnormalLabels(result.get("alarmMetrics"), abnormal);
        collectAbnormalLabels(result.get("dependencyMetrics"), abnormal);
        collectAbnormalLabels(result.get("traceMetrics"), abnormal);
        collectAbnormalLabels(result.get("instanceMetrics"), abnormal);
        collectAbnormalLabels(result.get("exceptionMetrics"), abnormal);
        collectAbnormalLabels(result.get("jvmMetrics"), abnormal);
        collectAbnormalLabels(result.get("resourceMetrics"), abnormal);
        if (abnormal.isEmpty()) {
            return "初步巡检未发现明显异常。";
        }
        return "初步巡检发现可疑异常：" + String.join("、", abnormal);
    }

    private static List<Double> extractSeveritySeries(Object severityCnts, Set<String> severities) {
        if (!(severityCnts instanceof Map<?, ?> buckets) || buckets.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<?, ?>> ordered = new ArrayList<>(buckets.entrySet());
        ordered.sort((left, right) -> String.valueOf(left.getKey()).compareTo(String.valueOf(right.getKey())));
        List<Double> values = new ArrayList<>(ordered.size());
        for (Map.Entry<?, ?> entry : ordered) {
            double sum = 0;
            if (entry.getValue() instanceof Map<?, ?> severityMap) {
                for (Map.Entry<?, ?> severityEntry : severityMap.entrySet()) {
                    if (matchesSeverity(String.valueOf(severityEntry.getKey()), severities)) {
                        sum += numberValueStatic(severityEntry.getValue());
                    }
                }
            }
            values.add(sum);
        }
        return values;
    }

    private static boolean matchesSeverity(String severity, Set<String> targets) {
        if (severity == null || severity.isBlank()) {
            return false;
        }
        if (targets.contains(severity)) {
            return true;
        }
        return targets.contains(severity.toUpperCase(Locale.ROOT))
                || targets.contains(severity.toLowerCase(Locale.ROOT));
    }

    private static List<String> serviceAliases(String serviceName, Map<String, Object> serviceInfo) {
        List<String> aliases = new ArrayList<>();
        aliases.add(serviceName);
        if (serviceInfo != null) {
            for (String key : List.of("service", "name", "serviceId")) {
                String value = stringValue(serviceInfo.get(key), "");
                if (!value.isBlank() && aliases.stream().noneMatch(value::equals)) {
                    aliases.add(value);
                }
            }
        }
        return aliases;
    }

    private static boolean matchesAnyAlias(String candidate, List<String> aliases) {
        if (candidate == null || candidate.isBlank() || aliases == null) {
            return false;
        }
        for (String alias : aliases) {
            if (PortalServiceIdResolver.matches(alias, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static double numberValueStatic(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static void collectAbnormalLabels(Object node, List<String> labels) {
        if (node instanceof Map<?, ?> map) {
            Object detection = map.get("detection");
            if (detection instanceof Map<?, ?> d && Boolean.TRUE.equals(d.get("anomaly"))) {
                Object label = map.get("label");
                labels.add(label == null ? "未知指标" : String.valueOf(label));
            }
            for (Object value : map.values()) {
                collectAbnormalLabels(value, labels);
            }
        }
    }

    private static boolean isWebService(String serviceType, Map<String, Object> serviceInfo) {
        if ("web".equalsIgnoreCase(serviceType) || "service".equalsIgnoreCase(serviceType)) {
            return true;
        }
        Object type = serviceInfo == null ? null : serviceInfo.get("type");
        return type != null && "web".equalsIgnoreCase(String.valueOf(type));
    }

    private static double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String stringValue(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private static String escapeLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private <T> CompletableFuture<T> submitInspection(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, inspectionExecutor);
    }

    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("inspection query failed", cause);
        }
    }

    private static ExecutorService createInspectionExecutor() {
        AtomicInteger threadSequence = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "service-inspection-" + threadSequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(
                INSPECTION_CONCURRENCY,
                INSPECTION_CONCURRENCY,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(INSPECTION_QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private record KeywordInspection(
            String keyword,
            long total,
            List<Map<String, Object>> samples) {
    }
}
