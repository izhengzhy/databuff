package com.databuff.apm.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Flushes {@link DorisBatchWriter} batches via {@link DorisStreamLoader}.
 * On Stream Load failure the batch is re-queued; after
 * {@link #DEFAULT_MAX_CONSECUTIVE_FAILURES} consecutive failures for this sink the batch is
 * dropped (fail-soft) so a poison pill cannot block the table indefinitely.
 */
public final class DorisStreamLoadSink {

    private static final Logger log = LoggerFactory.getLogger(DorisStreamLoadSink.class);

    /** Default consecutive Stream Load failures before dropping the current batch. */
    public static final int DEFAULT_MAX_CONSECUTIVE_FAILURES = 3;

    private final DorisBatchWriter batchWriter;
    private final DorisStreamLoader streamLoader;
    private final String database;
    private final String table;
    private final int maxConsecutiveFailures;

    private int consecutiveFailures;

    public DorisStreamLoadSink(
            DorisBatchWriter batchWriter,
            DorisStreamLoader streamLoader,
            String database,
            String table) {
        this(batchWriter, streamLoader, database, table, DEFAULT_MAX_CONSECUTIVE_FAILURES);
    }

    public DorisStreamLoadSink(
            DorisBatchWriter batchWriter,
            DorisStreamLoader streamLoader,
            String database,
            String table,
            int maxConsecutiveFailures) {
        this.batchWriter = Objects.requireNonNull(batchWriter);
        this.streamLoader = Objects.requireNonNull(streamLoader);
        this.database = Objects.requireNonNull(database);
        this.table = Objects.requireNonNull(table);
        this.maxConsecutiveFailures = Math.max(1, maxConsecutiveFailures);
    }

    public int flushReady() throws IOException {
        List<byte[]> batch = batchWriter.drainIfReady();
        return loadBatch(batch);
    }

    public int flushAll() throws IOException {
        List<byte[]> batch = batchWriter.flushAll();
        return loadBatch(batch);
    }

    private int loadBatch(List<byte[]> batch) throws IOException {
        if (batch.isEmpty()) {
            return 0;
        }
        byte[] body = joinJsonLines(batch);
        try {
            DorisStreamLoader.StreamLoadResult result = streamLoader.loadJsonLines(database, table, body);
            if (!result.success()) {
                String sample = sampleRow(batch);
                String hint = DorisTableNames.META_SERVICE.equals(table)
                        ? " (re-run deploy/common/sql/databuff.sql if meta_service schema is outdated)"
                        : "";
                logPipelineStreamLoad(table, batch.size(), sample, false, result.body());
                throw new IOException("Doris stream load failed" + hint + ": " + result.body()
                        + (sample.isEmpty() ? "" : " sampleRow=" + sample));
            }
            consecutiveFailures = 0;
            logPipelineStreamLoad(table, batch.size(), sampleRow(batch), true, result.body());
            log.debug("Stream loaded {} rows to {}.{}", batch.size(), database, table);
            return batch.size();
        } catch (IOException e) {
            consecutiveFailures++;
            if (consecutiveFailures < maxConsecutiveFailures) {
                batchWriter.offerAll(batch);
                throw e;
            }
            String sample = sampleRow(batch);
            log.error(
                    "Doris stream load dropped after {} consecutive failures {}.{} rows={} sample={} cause={}",
                    consecutiveFailures,
                    database,
                    table,
                    batch.size(),
                    sample,
                    e.toString());
            consecutiveFailures = 0;
            return 0;
        }
    }

    static byte[] joinJsonLines(List<byte[]> rows) {
        if (rows.isEmpty()) {
            return new byte[0];
        }
        int size = 0;
        for (byte[] row : rows) {
            size += row.length + 1;
        }
        byte[] out = new byte[size - 1];
        int pos = 0;
        for (int i = 0; i < rows.size(); i++) {
            byte[] row = rows.get(i);
            System.arraycopy(row, 0, out, pos, row.length);
            pos += row.length;
            if (i < rows.size() - 1) {
                out[pos++] = '\n';
            }
        }
        return out;
    }

    public String table() {
        return table;
    }

    public String database() {
        return database;
    }

    /** Visible for tests. */
    int consecutiveFailures() {
        return consecutiveFailures;
    }

    private static String sampleRow(List<byte[]> batch) {
        if (batch.isEmpty()) {
            return "";
        }
        String row = new String(batch.get(0), StandardCharsets.UTF_8);
        return row.length() > 500 ? row.substring(0, 500) + "..." : row;
    }

    private static void logPipelineStreamLoad(
            String table,
            int rowCount,
            String sampleRow,
            boolean success,
            String body) {
        if (!DorisTableNames.METRIC_JVM.equals(table)) {
            return;
        }
        String status = truncate(body, 300);
        if (success) {
            log.debug(
                    "[metric-pipeline] STREAM_LOAD table={} rows={} success=true sample={} doris={}",
                    table,
                    rowCount,
                    sampleRow,
                    status);
        } else {
            log.warn(
                    "[metric-pipeline] STREAM_LOAD table={} rows={} success=false sample={} doris={}",
                    table,
                    rowCount,
                    sampleRow,
                    status);
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
