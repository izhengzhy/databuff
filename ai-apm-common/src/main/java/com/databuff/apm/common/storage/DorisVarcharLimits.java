package com.databuff.apm.common.storage;

/**
 * Doris text column upper bounds (aligned with {@code databuff.sql} / V004 / V005).
 * Ingest truncates by {@link String#length()} so a single oversized value cannot fail an entire
 * Stream Load batch. VARCHAR limits are character caps chosen to stay under Doris UTF-8 byte
 * ceilings for typical payloads; {@link #LOG_BODY} is a soft cap for the STRING column.
 */
public final class DorisVarcharLimits {

    /** HTTP / connection-pool URLs ({@code url}, {@code meta.http.url}, {@code connectionPoolUrl}). */
    public static final int URL = 4096;

    /** Span {@code resource} and {@code meta.http.url} may hold full URLs. */
    public static final int SPAN_RESOURCE = 4096;

    /** Metric AGGREGATE KEY text tags: resource / sql / command / topic / etc. */
    public static final int RESOURCE = 1024;

    public static final int SQL_CONTENT = 1024;

    /** Log attribute / resource JSON blobs. */
    public static final int JSON_BLOB = 15000;

    public static final int SPAN_META = 10000;

    public static final int SPAN_METRICS = 1000;

    /**
     * Default max {@link String#length()} for {@code log_dc_record.body} (Doris STRING).
     * Override via {@code ingest.doris.log-body-max-length}. Align with BE
     * {@code string_type_length_soft_limit_bytes} when raising (CJK ≈ 3 UTF-8 bytes per char).
     */
    public static final int LOG_BODY = 1_048_576;

    private DorisVarcharLimits() {
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /** Truncates metric tag values known to be long-text columns in Doris. */
    public static String truncateMetricTag(String column, String value) {
        if (value == null || column == null) {
            return value;
        }
        return switch (column) {
            case "url", "connectionPoolUrl" -> truncate(value, URL);
            case "sqlContent" -> truncate(value, SQL_CONTENT);
            case "resource", "rootResource", "parentResource", "command", "topic" ->
                    truncate(value, RESOURCE);
            default -> value;
        };
    }
}
