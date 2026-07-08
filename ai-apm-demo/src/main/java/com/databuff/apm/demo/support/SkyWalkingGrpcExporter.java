package com.databuff.apm.demo.support;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.language.agent.v3.JVMMetricCollection;
import org.apache.skywalking.apm.network.language.agent.v3.JVMMetricReportServiceGrpc;
import org.apache.skywalking.apm.network.language.agent.v3.SegmentCollection;
import org.apache.skywalking.apm.network.language.agent.v3.TraceSegmentReportServiceGrpc;
import org.apache.skywalking.apm.network.logging.v3.LogData;
import org.apache.skywalking.apm.network.logging.v3.LogReportServiceGrpc;
import org.apache.skywalking.apm.network.management.v3.InstanceProperties;
import org.apache.skywalking.apm.network.management.v3.ManagementServiceGrpc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class SkyWalkingGrpcExporter {

    private SkyWalkingGrpcExporter() {
    }

    public static void registerInstanceOnce(String target) {
        HostPort endpoint = HostPort.parse(target);
        ManagedChannel channel = channel(endpoint);
        try {
            ManagementServiceGrpc.newBlockingStub(channel)
                    .reportInstanceProperties(InstanceProperties.newBuilder()
                            .setService("ai-apm-demo")
                            .setServiceInstance("demo-seeder-" + ProcessHandle.current().pid())
                            .addProperties(KeyStringValuePairUtil.of("demo", "skywalking-seeder"))
                            .build());
        } finally {
            shutdown(channel);
        }
    }

    public static void postSegments(String target, SegmentCollection collection) throws Exception {
        HostPort endpoint = HostPort.parse(target);
        ManagedChannel channel = channel(endpoint);
        try {
            Commands commands = TraceSegmentReportServiceGrpc.newBlockingStub(channel)
                    .collectInSync(collection);
            if (commands == null) {
                throw new IllegalStateException("SkyWalking trace export returned null Commands");
            }
        } finally {
            shutdown(channel);
        }
    }

    public static void postJvmMetrics(String target, JVMMetricCollection collection) throws Exception {
        HostPort endpoint = HostPort.parse(target);
        ManagedChannel channel = channel(endpoint);
        try {
            Commands commands = JVMMetricReportServiceGrpc.newBlockingStub(channel).collect(collection);
            if (commands == null) {
                throw new IllegalStateException("SkyWalking JVM export returned null Commands");
            }
        } finally {
            shutdown(channel);
        }
    }

    public static void postLogs(String target, List<LogData> logs) throws Exception {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        HostPort endpoint = HostPort.parse(target);
        ManagedChannel channel = channel(endpoint);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        try {
            LogReportServiceGrpc.LogReportServiceStub stub = LogReportServiceGrpc.newStub(channel);
            io.grpc.stub.StreamObserver<LogData> requestObserver = stub.collect(new io.grpc.stub.StreamObserver<>() {
                @Override
                public void onNext(Commands value) {
                }

                @Override
                public void onError(Throwable t) {
                    error.set(t);
                    done.countDown();
                }

                @Override
                public void onCompleted() {
                    done.countDown();
                }
            });
            for (LogData log : logs) {
                requestObserver.onNext(log);
            }
            requestObserver.onCompleted();
            if (!done.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("SkyWalking log export timed out");
            }
            if (error.get() != null) {
                throw new IllegalStateException("SkyWalking log export failed", error.get());
            }
        } finally {
            shutdown(channel);
        }
    }

    private static ManagedChannel channel(HostPort endpoint) {
        return ManagedChannelBuilder.forAddress(endpoint.host(), endpoint.port())
                .usePlaintext()
                .build();
    }

    private static void shutdown(ManagedChannel channel) {
        channel.shutdown();
        try {
            channel.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record HostPort(String host, int port) {
        static HostPort parse(String target) {
            String normalized = target == null ? "" : target.trim();
            if (normalized.startsWith("http://")) {
                normalized = normalized.substring("http://".length());
            }
            if (normalized.startsWith("https://")) {
                normalized = normalized.substring("https://".length());
            }
            int colon = normalized.lastIndexOf(':');
            if (colon <= 0 || colon == normalized.length() - 1) {
                throw new IllegalArgumentException("invalid SkyWalking gRPC target: " + target);
            }
            return new HostPort(normalized.substring(0, colon), Integer.parseInt(normalized.substring(colon + 1)));
        }
    }
}
