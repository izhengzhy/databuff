package com.databuff.apm.ingest.metric;

import com.databuff.apm.common.storage.DorisTableNames;
import com.databuff.apm.ingest.meta.MetaServiceCollector;
import com.databuff.apm.ingest.otel.OtlMetricLine;
import com.databuff.apm.ingest.otel.OtlpMetricDebugLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Maps OTLP metric lines and writes them directly to Doris batch writers.
 * JVM rows from the same export request are merged by aggregate key so partial
 * Stream Load rows do not null out other {@code metric_jvm} columns.
 */
public final class OtlpMetricDirectWriter {

    private static final Logger log = LoggerFactory.getLogger(OtlpMetricDirectWriter.class);
    private static final List<String> JVM_KEY_COLUMNS = List.of(
            "metric_time", "ts", "instance", "service", "service_id", "service_instance", "tag_host");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final MetricWriteRouter metricWriteRouter;
    private final MetaServiceCollector metaServiceCollector;

    public OtlpMetricDirectWriter(MetricWriteRouter metricWriteRouter, MetaServiceCollector metaServiceCollector) {
        this.metricWriteRouter = metricWriteRouter;
        this.metaServiceCollector = metaServiceCollector;
    }

    public OtlpMetricDirectWriter(MetricWriteRouter metricWriteRouter) {
        this(metricWriteRouter, null);
    }

    public void write(List<OtlMetricLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        Map<String, Map<String, Object>> jvmRows = new LinkedHashMap<>();
        Map<String, Integer> jvmPartialCounts = new LinkedHashMap<>();
        int skippedMap = 0;
        for (OtlMetricLine line : lines) {
            if (metaServiceCollector != null) {
                metaServiceCollector.remember(line);
            }
            Optional<OtlpMetricRowMapper.MappedRow> mapped = OtlpMetricRowMapper.map(line);
            if (mapped.isEmpty()) {
                skippedMap++;
                OtlpMetricDebugLogger.mapSkipped(line, "no mapped row");
                continue;
            }
            OtlpMetricRowMapper.MappedRow row = mapped.get();
            if (DorisTableNames.METRIC_JVM.equals(row.table())) {
                try {
                    mergeJvmRow(jvmRows, jvmPartialCounts, row.row());
                } catch (IOException e) {
                    log.warn("Failed to merge JVM OTLP row for {}: {}", line.service(), e.getMessage());
                }
                continue;
            }
            metricWriteRouter.offerMappedRow(row);
        }
        String sampleService = lines.stream()
                .map(OtlMetricLine::service)
                .filter(service -> service != null && !service.isBlank())
                .findFirst()
                .orElse("");
        int jvmLines = (int) lines.stream()
                .filter(line -> line.metric() != null && line.metric().startsWith("jvm."))
                .count();
        OtlpMetricDebugLogger.ingestBatch(sampleService, lines.size(), jvmLines, skippedMap);
        for (Map.Entry<String, Map<String, Object>> entry : jvmRows.entrySet()) {
            Map<String, Object> row = entry.getValue();
            Set<String> metricFields = metricFieldNames(row);
            OtlpMetricDebugLogger.mergedJvmRow(
                    String.valueOf(row.getOrDefault("service", "")),
                    String.valueOf(row.getOrDefault("service_id", "")),
                    jvmPartialCounts.getOrDefault(entry.getKey(), 0),
                    metricFields);
            try {
                byte[] bytes = JSON.writeValueAsBytes(row);
                metricWriteRouter.offerJvmRow(bytes);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize merged JVM row: {}", e.getMessage());
            }
        }
    }

    private static void mergeJvmRow(
            Map<String, Map<String, Object>> pending,
            Map<String, Integer> partialCounts,
            byte[] rowBytes) throws IOException {
        JsonNode node = JSON.readTree(rowBytes);
        String key = aggregateKey(node);
        partialCounts.merge(key, 1, Integer::sum);
        Map<String, Object> merged = pending.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value != null && !value.isNull()) {
                merged.put(entry.getKey(), JSON.convertValue(value, Object.class));
            }
        });
    }

    private static Set<String> metricFieldNames(Map<String, Object> row) {
        Set<String> fields = new TreeSet<>();
        for (String key : row.keySet()) {
            if (!JVM_KEY_COLUMNS.contains(key)) {
                fields.add(key);
            }
        }
        return fields;
    }

    private static String aggregateKey(JsonNode node) {
        StringBuilder key = new StringBuilder();
        for (String column : JVM_KEY_COLUMNS) {
            if (!key.isEmpty()) {
                key.append('\u0001');
            }
            key.append(column).append('=').append(node.path(column).asText(""));
        }
        return key.toString();
    }
}
