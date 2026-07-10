package com.databuff.apm.ingest.trace;

import com.databuff.apm.common.model.DcSpan;
import com.databuff.apm.common.model.OptimizedMetric;
import com.databuff.apm.common.serde.DcSpanUtil;
import com.databuff.apm.ingest.trace.remote.RemoteCallProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Step 4–5 · trace fill 与指标提取。
 * <p>
 * 1. fill 上下游父子关系、serviceflow 字段<br>
 * 2. DTS 远程服务识别（{@link RemoteCallProcessor}）<br>
 * 3. 组件虚拟服务提取（DB/MQ/Redis 等）<br>
 * 4. {@link DcSpanUtil#parseSpanData} 提取 OptimizedMetric<br>
 * 5. span 序列化为 JSON 行（仅此处 encode，供 dc_span 存储）
 */
public final class TraceFillProcessor {

    private final VirtualServiceExtractor virtualServiceExtractor;
    private final RemoteCallProcessor remoteCallProcessor;

    public TraceFillProcessor() {
        this(null, null);
    }

    public TraceFillProcessor(VirtualServiceExtractor virtualServiceExtractor) {
        this(virtualServiceExtractor, null);
    }

    public TraceFillProcessor(
            VirtualServiceExtractor virtualServiceExtractor,
            RemoteCallProcessor remoteCallProcessor) {
        this.virtualServiceExtractor = virtualServiceExtractor;
        this.remoteCallProcessor = remoteCallProcessor;
    }

    public record FillResult(List<byte[]> filledSpanBytes, List<OptimizedMetric> metrics) {
    }

    public FillResult processTrace(List<DcSpan> spans) throws IOException {
        FillPathAndRelationUtil.fillRelations(spans);
        if (remoteCallProcessor != null) {
            remoteCallProcessor.processAfterFill(spans);
        }
        if (virtualServiceExtractor != null) {
            virtualServiceExtractor.extractFromTrace(spans);
        }
        List<byte[]> encoded = FillPathAndRelationUtil.encodeFilled(spans);
        List<OptimizedMetric> metrics = new ArrayList<>();
        if (ServiceFlowExtractor.hasTraceEntryRoot(spans)) {
            metrics.addAll(ServiceFlowExtractor.extractFromTrace(spans));
        }
        for (DcSpan span : spans) {
            metrics.addAll(DcSpanUtil.parseSpanData(span));
        }
        return new FillResult(encoded, metrics);
    }
}
