package com.databuff.apm.demo.support;

import org.apache.skywalking.apm.network.logging.v3.LogData;
import org.apache.skywalking.apm.network.logging.v3.LogDataBody;
import org.apache.skywalking.apm.network.logging.v3.LogTags;
import org.apache.skywalking.apm.network.logging.v3.TextLog;
import org.apache.skywalking.apm.network.logging.v3.TraceContext;

import java.util.ArrayList;
import java.util.List;

/** SkyWalking logs aligned with {@link OtlpLogFixture} for integration test parity. */
public final class SkyWalkingLogFixture {

    private SkyWalkingLogFixture() {
    }

    public static List<LogData> logsForBatch(DemoSkyWalkingBatch batch) {
        List<LogData> logs = new ArrayList<>();
        DemoSkyWalkingBatch.SpanRef root = batch.serviceARoot();
        logs.add(log(root, batch.traceId(), root.timeMs(), "INFO",
                "Received checkout request orderId=10001 channel=web"));
        logs.add(log(root, batch.traceId(), root.timeMs() + 3, "INFO",
                "Validating cart contents for user demo-user"));
        logs.add(log(root, batch.traceId(), root.timeMs() + 8, "INFO",
                "Checkout started orderId=10001"));
        logs.add(log(root, batch.traceId(), root.timeMs() + 15, "INFO",
                "Delegating inventory check to service-b"));

        DemoSkyWalkingBatch.SpanRef httpClient = batch.serviceAHttpClient();
        logs.add(log(httpClient, batch.traceId(), httpClient.timeMs(), "INFO",
                "HTTP client calling service-b GET /api/orders/10001"));
        logs.add(log(httpClient, batch.traceId(), httpClient.timeMs() + 12, "INFO",
                "service-b responded 200 in 95ms"));

        DemoSkyWalkingBatch.SpanRef httpServer = batch.serviceBHttpServer();
        logs.add(log(httpServer, batch.traceId(), httpServer.timeMs(), "INFO",
                "GET /api/orders/10001 from service-a"));
        logs.add(log(httpServer, batch.traceId(), httpServer.timeMs() + 8, "INFO",
                "Loaded order 10001 from demo_order"));
        logs.add(log(httpServer, batch.traceId(), httpServer.timeMs() + 22, "INFO",
                "Order status=CONFIRMED amount=199.00"));

        DemoSkyWalkingBatch.SpanRef dubboMysql = batch.serviceBDbSpan();
        logs.add(log(dubboMysql, batch.traceId(), dubboMysql.timeMs(), "INFO",
                "Querying inventory for sku DEMO-10001"));
        logs.add(log(dubboMysql, batch.traceId(), dubboMysql.timeMs() + 6, "WARN",
                "Available stock below threshold (2 units)"));
        logs.add(log(dubboMysql, batch.traceId(), dubboMysql.timeMs() + 12, "ERROR",
                "InsufficientStockException: inventory unavailable for sku DEMO-10001"));
        return logs;
    }

    private static LogData log(
            DemoSkyWalkingBatch.SpanRef span, String traceId, long timeMs, String level, String body) {
        return LogData.newBuilder()
                .setTimestamp(timeMs)
                .setService(span.service())
                .setServiceInstance(span.serviceInstance())
                .setBody(LogDataBody.newBuilder().setText(TextLog.newBuilder().setText(body)))
                .setTraceContext(TraceContext.newBuilder()
                        .setTraceId(traceId)
                        .setTraceSegmentId(span.segmentId())
                        .setSpanId(span.spanId()))
                .setTags(LogTags.newBuilder()
                        .addData(KeyStringValuePairUtil.of("level", level))
                        .addData(KeyStringValuePairUtil.of("host.name", span.hostName()))
                        .addData(KeyStringValuePairUtil.of("k8s.namespace.name", "demo"))
                        .addData(KeyStringValuePairUtil.of("order.id", "10001")))
                .build();
    }
}
