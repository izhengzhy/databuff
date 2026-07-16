package com.databuff.apm.ingest.log;

import com.databuff.apm.common.storage.DorisBatchWriter;
import com.databuff.apm.common.storage.DorisVarcharLimits;
import com.databuff.apm.ingest.meta.MetaServiceCollector;
import com.databuff.apm.ingest.otel.OtlLogLine;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Maps OTLP log lines and writes them directly to Doris via Stream Load batching. */
public final class OtlpLogDirectWriter {

    private static final Logger log = LoggerFactory.getLogger(OtlpLogDirectWriter.class);

    private final DorisBatchWriter logBatchWriter;
    private final MetaServiceCollector metaServiceCollector;
    private final int logBodyMaxLength;

    public OtlpLogDirectWriter(DorisBatchWriter logBatchWriter, MetaServiceCollector metaServiceCollector) {
        this(logBatchWriter, metaServiceCollector, DorisVarcharLimits.LOG_BODY);
    }

    public OtlpLogDirectWriter(
            DorisBatchWriter logBatchWriter,
            MetaServiceCollector metaServiceCollector,
            int logBodyMaxLength) {
        this.logBatchWriter = logBatchWriter;
        this.metaServiceCollector = metaServiceCollector;
        this.logBodyMaxLength = logBodyMaxLength > 0 ? logBodyMaxLength : DorisVarcharLimits.LOG_BODY;
    }

    public void write(List<OtlLogLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        int skipped = 0;
        for (OtlLogLine line : lines) {
            if (line == null) {
                skipped++;
                continue;
            }
            if (metaServiceCollector != null) {
                metaServiceCollector.remember(line);
            }
            try {
                logBatchWriter.offer(line.toJsonBytes(logBodyMaxLength));
            } catch (JsonProcessingException e) {
                skipped++;
                log.warn("Failed to serialize OTLP log row for {}: {}", line.service(), e.getMessage());
            }
        }
        if (skipped > 0) {
            log.debug("OTLP log write skipped {} rows", skipped);
        }
    }
}
