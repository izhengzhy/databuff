package com.databuff.apm.web.portal;

import com.databuff.apm.common.query.ApmQueryModels.SpanDetail;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo checkout trace spans for portal integration tests, aligned with
 * {@code OtlpTraceFixture#sampleTraceExport} and ingest {@code DemoTraceSpans}.
 */
final class DemoCheckoutTraceDetailFixture {

    static final String TRACE_ID = "trace-demo";
    private static final long TRACE_START = 1_700_000_000_000_000_000L;
    private static final String START_TIME = "2026-06-01 12:00:00";
    private static final String SERVICE_A = "service-a";
    private static final String SERVICE_A_ID = "9bf61532d56eb7b5";
    private static final String SERVICE_B = "service-b";
    private static final String SERVICE_B_ID = "5457a0119281bb98";

    private DemoCheckoutTraceDetailFixture() {
    }

    static List<SpanDetail> checkoutTraceDetails() {
        List<SpanDetail> spans = new ArrayList<>();

        spans.add(httpServer(
                "root-a", "", SERVICE_A, SERVICE_A_ID, TRACE_START, 240_000_000L,
                "GET /demo/checkout", "GET /demo/checkout", "/demo/checkout"));

        spans.add(dbClient(
                "redis-client", "root-a", SERVICE_A, SERVICE_A_ID, TRACE_START + 5_000_000L, 13_000_000L,
                "redis", "GET cart:10001"));
        spans.add(httpClient(
                "remote-http", "root-a", SERVICE_A, SERVICE_A_ID, TRACE_START + 12_000_000L, 7_000_000L,
                "https://payments.example.com/api/risk/check"));
        spans.add(httpClient(
                "http-client", "root-a", SERVICE_A, SERVICE_A_ID, TRACE_START + 20_000_000L, 100_000_000L,
                "http://service-b:8080/api/orders/10001"));
        spans.add(esClient("es-client", "root-a", SERVICE_A, SERVICE_A_ID, TRACE_START + 100_000_000L, 18_000_000L));
        spans.add(rpcClient(
                "dubbo-client", "root-a", SERVICE_A, SERVICE_A_ID, TRACE_START + 125_000_000L, 80_000_000L));
        spans.add(dbClient(
                "audit-mysql", "root-a", SERVICE_A, SERVICE_A_ID, TRACE_START + 208_000_000L, 20_000_000L,
                "mysql", "INSERT INTO demo_order_audit(order_id, channel) VALUES (?, ?)"));
        spans.add(mqClient(
                "kafka-client", "root-a", SERVICE_A, SERVICE_A_ID, TRACE_START + 230_000_000L, 8_000_000L));

        spans.add(httpServer(
                "http-server", "http-client", SERVICE_B, SERVICE_B_ID, TRACE_START + 30_000_000L, 80_000_000L,
                "GET /api/orders/{orderId}", "GET /api/orders/{orderId}", "/api/orders/10001"));
        spans.add(dbClient(
                "http-mysql", "http-server", SERVICE_B, SERVICE_B_ID, TRACE_START + 45_000_000L, 45_000_000L,
                "mysql", "SELECT demo_order"));
        spans.add(rpcServer(
                "dubbo-server", "dubbo-client", SERVICE_B, SERVICE_B_ID, TRACE_START + 135_000_000L, 60_000_000L));
        spans.add(dbClient(
                "dubbo-mysql", "dubbo-server", SERVICE_B, SERVICE_B_ID, TRACE_START + 150_000_000L, 30_000_000L,
                "mysql", "SELECT demo_inventory", 1));
        spans.add(dbClient(
                "redis-server-b", "http-server", SERVICE_B, SERVICE_B_ID, TRACE_START + 95_000_000L, 13_000_000L,
                "redis", "SET order:10001"));
        return spans;
    }

    private static SpanDetail httpServer(
            String spanId,
            String parentId,
            String service,
            String serviceId,
            long start,
            long duration,
            String name,
            String resource,
            String httpUrl) {
        return span(
                spanId,
                parentId,
                service,
                serviceId,
                start,
                duration,
                0,
                name,
                resource,
                "SPAN_KIND_SERVER",
                1,
                0,
                "{}",
                200,
                "GET",
                httpUrl);
    }

    private static SpanDetail httpClient(
            String spanId, String parentId, String service, String serviceId, long start, long duration, String url) {
        return span(
                spanId,
                parentId,
                service,
                serviceId,
                start,
                duration,
                0,
                "HTTP GET " + url,
                "HTTP GET " + url,
                "SPAN_KIND_CLIENT",
                0,
                1,
                "{\"http.method\":\"GET\",\"http.status_code\":\"200\",\"url.full\":\"" + url + "\"}",
                200,
                "GET",
                url);
    }

    private static SpanDetail dbClient(
            String spanId,
            String parentId,
            String service,
            String serviceId,
            long start,
            long duration,
            String dbSystem,
            String statement) {
        return dbClient(spanId, parentId, service, serviceId, start, duration, dbSystem, statement, 0);
    }

    private static SpanDetail dbClient(
            String spanId,
            String parentId,
            String service,
            String serviceId,
            long start,
            long duration,
            String dbSystem,
            String statement,
            int error) {
        String meta;
        if ("mysql".equals(dbSystem)) {
            meta = "{\"db.system\":\"mysql\",\"db.name\":\"demo_apm\",\"db.statement\":\"" + statement + "\","
                    + "\"server.address\":\"mysql\",\"server.port\":\"3306\"}";
        } else {
            meta = "{\"db.system\":\"" + dbSystem + "\",\"db.statement\":\"" + statement + "\","
                    + "\"server.address\":\"redis\",\"server.port\":\"6379\"}";
        }
        return span(
                spanId,
                parentId,
                service,
                serviceId,
                start,
                duration,
                error,
                statement,
                statement,
                "SPAN_KIND_CLIENT",
                0,
                1,
                meta,
                null,
                null,
                null);
    }

    private static SpanDetail esClient(
            String spanId, String parentId, String service, String serviceId, long start, long duration) {
        return span(
                spanId,
                parentId,
                service,
                serviceId,
                start,
                duration,
                0,
                "orders/_search",
                "orders/_search",
                "SPAN_KIND_CLIENT",
                0,
                1,
                "{\"db.system\":\"elasticsearch\",\"db.elasticsearch.index\":\"orders\","
                        + "\"http.method\":\"GET\",\"url.full\":\"http://es:9200/orders/_search\","
                        + "\"server.address\":\"es\",\"server.port\":\"9200\"}",
                null,
                "GET",
                "http://es:9200/orders/_search");
    }

    private static SpanDetail mqClient(
            String spanId, String parentId, String service, String serviceId, long start, long duration) {
        return span(
                spanId,
                parentId,
                service,
                serviceId,
                start,
                duration,
                0,
                "order-events publish",
                "order-events publish",
                "SPAN_KIND_CLIENT",
                0,
                1,
                "{\"messaging.system\":\"kafka\",\"messaging.destination.name\":\"order-events\","
                        + "\"messaging.operation\":\"publish\",\"net.peer.name\":\"kafka\",\"server.port\":\"9092\"}",
                null,
                null,
                null);
    }

    private static SpanDetail rpcClient(
            String spanId, String parentId, String service, String serviceId, long start, long duration) {
        return span(
                spanId,
                parentId,
                service,
                serviceId,
                start,
                duration,
                0,
                "Dubbo DemoOrderService.findInventory",
                "Dubbo DemoOrderService.findInventory",
                "SPAN_KIND_CLIENT",
                0,
                1,
                "{\"rpc.system\":\"dubbo\",\"rpc.service\":\"com.databuff.demo.OrderService\","
                        + "\"rpc.method\":\"findInventory\",\"net.peer.name\":\"service-b\",\"net.peer.port\":\"20880\"}",
                null,
                null,
                null);
    }

    private static SpanDetail rpcServer(
            String spanId, String parentId, String service, String serviceId, long start, long duration) {
        return span(
                spanId,
                parentId,
                service,
                serviceId,
                start,
                duration,
                0,
                "Dubbo DemoOrderService.findInventory",
                "Dubbo DemoOrderService.findInventory",
                "SPAN_KIND_SERVER",
                1,
                0,
                "{\"rpc.system\":\"dubbo\",\"rpc.service\":\"com.databuff.demo.OrderService\","
                        + "\"rpc.method\":\"findInventory\"}",
                null,
                null,
                null);
    }

    private static SpanDetail span(
            String spanId,
            String parentId,
            String service,
            String serviceId,
            long start,
            long duration,
            int error,
            String name,
            String resource,
            String type,
            int isIn,
            int isOut,
            String meta,
            Integer metaHttpStatusCode,
            String metaHttpMethod,
            String metaHttpUrl) {
        return new SpanDetail(
                TRACE_ID,
                spanId,
                parentId,
                service,
                serviceId,
                name,
                START_TIME,
                start,
                duration,
                error,
                "demo-host",
                service + "-1",
                resource,
                type,
                isIn,
                isOut,
                meta,
                null,
                metaHttpStatusCode,
                metaHttpMethod,
                metaHttpUrl,
                null);
    }
}
