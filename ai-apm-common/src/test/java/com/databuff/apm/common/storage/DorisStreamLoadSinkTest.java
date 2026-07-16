package com.databuff.apm.common.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DorisStreamLoadSinkTest {

    @Test
    void joinsJsonLinesWithNewline() {
        byte[] joined = DorisStreamLoadSink.joinJsonLines(List.of("{\"a\":1}".getBytes(), "{\"b\":2}".getBytes()));
        assertThat(new String(joined)).isEqualTo("{\"a\":1}\n{\"b\":2}");
    }

    @Test
    void flushAllReturnsZeroWhenEmpty() throws Exception {
        DorisStreamLoadSink sink = new DorisStreamLoadSink(
                new DorisBatchWriter(2),
                new DorisStreamLoader(new DorisConnectionConfig("localhost", 9030, 8030), "root", ""),
                "databuff",
                "metric_service");
        assertThat(sink.flushAll()).isZero();
    }

    @Test
    void flushReadyLoadsWhenBatchFull() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/api/databuff/metric_service/_stream_load", exchange -> {
            calls.incrementAndGet();
            byte[] ok = "{\"Status\": \"Success\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            try (java.io.OutputStream os = exchange.getResponseBody()) {
                os.write(ok);
            }
        });
        server.start();
        try {
            DorisStreamLoader loader = new DorisStreamLoader(
                    new DorisConnectionConfig("127.0.0.1", 9030, port), "root", "");
            DorisBatchWriter writer = new DorisBatchWriter(2);
            DorisStreamLoadSink sink = new DorisStreamLoadSink(writer, loader, "databuff", "metric_service");
            writer.offer("{\"a\":1}".getBytes());
            assertThat(sink.flushReady()).isZero();
            writer.offer("{\"a\":2}".getBytes());
            assertThat(sink.flushReady()).isEqualTo(2);
            assertThat(calls.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void exposesDatabaseAndTable() {
        DorisStreamLoadSink sink = new DorisStreamLoadSink(
                new DorisBatchWriter(1),
                new DorisStreamLoader(new DorisConnectionConfig("h", 9030, 8030), "root", ""),
                "databuff",
                "trace_dc_span");
        assertThat(sink.database()).isEqualTo("databuff");
        assertThat(sink.table()).isEqualTo("trace_dc_span");
    }

    @Test
    void requeuesOnFailureUntilBudgetThenDrops() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/api/databuff/log_dc_record/_stream_load", exchange -> {
            calls.incrementAndGet();
            byte[] fail = "{\"Status\": \"Fail\", \"Message\": \"too long\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, fail.length);
            try (java.io.OutputStream os = exchange.getResponseBody()) {
                os.write(fail);
            }
        });
        server.start();
        try {
            DorisStreamLoader loader = new DorisStreamLoader(
                    new DorisConnectionConfig("127.0.0.1", 9030, port), "root", "");
            DorisBatchWriter writer = new DorisBatchWriter(10);
            DorisStreamLoadSink sink = new DorisStreamLoadSink(writer, loader, "databuff", "log_dc_record", 3);
            writer.offer("{\"body\":\"x\"}".getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(sink::flushAll).isInstanceOf(IOException.class);
            assertThat(writer.pendingCount()).isEqualTo(1);
            assertThat(sink.consecutiveFailures()).isEqualTo(1);

            assertThatThrownBy(sink::flushAll).isInstanceOf(IOException.class);
            assertThat(writer.pendingCount()).isEqualTo(1);
            assertThat(sink.consecutiveFailures()).isEqualTo(2);

            assertThat(sink.flushAll()).isZero();
            assertThat(writer.pendingCount()).isZero();
            assertThat(sink.consecutiveFailures()).isZero();
            assertThat(calls.get()).isEqualTo(3);
        } finally {
            server.stop(0);
        }
    }
}
