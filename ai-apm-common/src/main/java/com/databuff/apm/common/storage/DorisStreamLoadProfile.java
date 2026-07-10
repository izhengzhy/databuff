package com.databuff.apm.common.storage;

import com.databuff.apm.common.metric.MetricSchemaRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-table Stream Load HTTP headers (Doris FE/BE {@code _stream_load}). */
public record DorisStreamLoadProfile(Map<String, String> headers) {

    private static final List<String> JVM_TAG_COLUMNS = List.of(
            "metric_time", "ts", "instance", "service", "service_id", "service_instance", "tag_host");

    public static DorisStreamLoadProfile forTable(String table) {
        if (DorisTableNames.META_SERVICE.equals(table)) {
            return metaService();
        }
        if (DorisTableNames.METRIC_JVM.equals(table)) {
            return metricJvm();
        }
        return new DorisStreamLoadProfile(Map.of("strict_mode", "false"));
    }

    /**
     * Maps JSON keys to {@code meta_service} columns; backticks cover reserved names
     * ({@code service}, {@code type}, {@code describe}).
     */
    public static DorisStreamLoadProfile metaService() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("strict_mode", "false");
        headers.put(
                "columns",
                "id,name,`service`,service_type,apikey,custom_tags,`type`,fqdn,`source`,`describe`,"
                        + "container_service,virtual_service,processRuntimeVersion,processRuntimeName,"
                        + "language,datasource,technology,update_time");
        headers.put(
                "jsonpaths",
                "[\"$.id\",\"$.name\",\"$.service\",\"$.service_type\",\"$.apikey\",\"$.custom_tags\","
                        + "\"$.type\",\"$.fqdn\",\"$.source\",\"$.describe\",\"$.container_service\","
                        + "\"$.virtual_service\",\"$.processRuntimeVersion\",\"$.processRuntimeName\","
                        + "\"$.language\",\"$.datasource\",\"$.technology\",\"$.update_time\"]");
        return new DorisStreamLoadProfile(Map.copyOf(headers));
    }

    /**
     * JVM metrics use underscore JSON keys aligned with {@code metric_jvm} column names.
     * Doris Stream Load cannot map jsonpaths into dotted column names via {@code columns}
     * expressions, so physical columns use underscores (see {@code databuff.sql}).
     */
    public static DorisStreamLoadProfile metricJvm() {
        MetricSchemaRegistry.schema("jvm").orElseThrow();
        List<String> columnExprs = new ArrayList<>(JVM_TAG_COLUMNS);
        List<String> jsonPaths = new ArrayList<>();
        for (String tag : JVM_TAG_COLUMNS) {
            jsonPaths.add("$." + tag);
        }
        MetricSchemaRegistry.schema("jvm").ifPresent(schema -> {
            for (String column : schema.fields()) {
                String jsonKey = MetricSchemaRegistry.streamLoadJsonKey(column);
                columnExprs.add(jsonKey);
                jsonPaths.add("$." + jsonKey);
            }
        });
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("strict_mode", "false");
        headers.put("columns", String.join(",", columnExprs));
        headers.put("jsonpaths", jsonPaths(jsonPaths));
        return new DorisStreamLoadProfile(Map.copyOf(headers));
    }

    private static String jsonPaths(List<String> paths) {
        return "[\"" + String.join("\",\"", paths) + "\"]";
    }
}
