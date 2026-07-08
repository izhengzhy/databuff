package com.databuff.apm.demo;

import com.databuff.apm.demo.support.DemoTraceBatch;
import com.databuff.apm.demo.support.OtlpLogFixture;
import com.databuff.apm.demo.support.OtlpTraceFixture;
import com.databuff.apm.demo.support.SkyWalkingDemoSeeder;

/**
 * Continuous demo seeder for OTLP and/or SkyWalking native protocol.
 *
 * <p>{@code SEED_PROTOCOL}: {@code otlp} (default), {@code skywalking}, or {@code both}
 */
public final class DemoOrderSeeder {

    private DemoOrderSeeder() {
    }

    public static void main(String[] args) throws Exception {
        String protocol = System.getenv().getOrDefault("SEED_PROTOCOL", "otlp").trim().toLowerCase();
        long traceIntervalSeconds = Long.parseLong(System.getenv().getOrDefault("SEED_INTERVAL_SECONDS", "30"));
        long jvmMetricIntervalSeconds = Long.parseLong(
                System.getenv().getOrDefault("JVM_METRIC_INTERVAL_SECONDS", "60"));

        boolean seedOtlp = protocol.equals("otlp") || protocol.equals("both");
        boolean seedSkyWalking = protocol.equals("skywalking") || protocol.equals("both");

        String otlpEndpoint = System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://127.0.0.1:4318");
        String skyWalkingTarget = System.getenv().getOrDefault("SKYWALKING_GRPC_TARGET", "127.0.0.1:31800");

        System.out.println("[demo-seeder] protocol=" + protocol + " pid=" + ProcessHandle.current().pid()
                + " traceInterval=" + traceIntervalSeconds + "s jvmInterval=" + jvmMetricIntervalSeconds + "s");
        if (seedOtlp) {
            System.out.println("[demo-seeder] OTLP -> " + otlpEndpoint);
        }
        if (seedSkyWalking) {
            System.out.println("[demo-seeder] SkyWalking gRPC -> " + skyWalkingTarget);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                System.out.println("[demo-seeder] shutdown (signal or JVM exit)"), "demo-seeder-shutdown"));

        if (seedSkyWalking && !seedOtlp) {
            SkyWalkingDemoSeeder.runLoop(skyWalkingTarget, traceIntervalSeconds, jvmMetricIntervalSeconds);
            return;
        }

        long sentTraces = 0;
        long sentLogs = 0;
        long sentMetrics = 0;
        long sentSkyWalking = 0;
        long lastMetricAtMillis = 0L;
        long jvmMetricIntervalMillis = jvmMetricIntervalSeconds * 1000L;
        while (true) {
            try {
                if (seedOtlp) {
                    DemoTraceBatch batch = OtlpTraceFixture.nextTraceBatch();
                    int traceStatus = OtlpTraceFixture.postTraceBatch(otlpEndpoint, batch);
                    if (traceStatus < 200 || traceStatus >= 300) {
                        System.err.println("[demo-seeder] OTLP trace HTTP " + traceStatus);
                    } else {
                        sentTraces++;
                        if (sentTraces == 1 || sentTraces % 20 == 0) {
                            System.out.println("[demo-seeder] OTLP traces sent=" + sentTraces);
                        }
                    }

                    int logStatus = OtlpLogFixture.postLogs(otlpEndpoint, batch);
                    if (logStatus < 200 || logStatus >= 300) {
                        System.err.println("[demo-seeder] OTLP log HTTP " + logStatus);
                    } else {
                        sentLogs++;
                    }

                    long now = System.currentTimeMillis();
                    if (lastMetricAtMillis == 0L || now - lastMetricAtMillis >= jvmMetricIntervalMillis) {
                        int metricStatus = OtlpTraceFixture.postMetrics(otlpEndpoint);
                        lastMetricAtMillis = now;
                        if (metricStatus < 200 || metricStatus >= 300) {
                            System.err.println("[demo-seeder] OTLP metric HTTP " + metricStatus);
                        } else {
                            sentMetrics++;
                        }
                    }
                }

                if (seedSkyWalking) {
                    SkyWalkingDemoSeeder.seedOnce(skyWalkingTarget);
                    sentSkyWalking++;
                    if (sentSkyWalking == 1 || sentSkyWalking % 20 == 0) {
                        System.out.println("[demo-seeder] SkyWalking batches sent=" + sentSkyWalking);
                    }
                }

                Thread.sleep(traceIntervalSeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[demo-seeder] interrupted, exiting");
                return;
            } catch (Exception e) {
                System.err.println("[demo-seeder] seed failed: " + e.getMessage());
                try {
                    Thread.sleep(traceIntervalSeconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.out.println("[demo-seeder] interrupted during backoff, exiting");
                    return;
                }
            }
        }
    }
}
