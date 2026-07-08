package com.databuff.apm.demo.support;

import org.apache.skywalking.apm.network.common.v3.CPU;
import org.apache.skywalking.apm.network.language.agent.v3.Class;
import org.apache.skywalking.apm.network.language.agent.v3.GC;
import org.apache.skywalking.apm.network.language.agent.v3.GCPhase;
import org.apache.skywalking.apm.network.language.agent.v3.JVMMetric;
import org.apache.skywalking.apm.network.language.agent.v3.JVMMetricCollection;
import org.apache.skywalking.apm.network.language.agent.v3.Memory;
import org.apache.skywalking.apm.network.language.agent.v3.MemoryPool;
import org.apache.skywalking.apm.network.language.agent.v3.PoolType;
import org.apache.skywalking.apm.network.language.agent.v3.Thread;

import java.time.Instant;
import java.util.List;

/** SkyWalking JVM metrics aligned with {@link JvmMetricSimulator} demo profiles. */
public final class SkyWalkingJvmFixture {

    private static long serviceAMinorGcCount = 150;
    private static long serviceAMajorGcCount = 8;
    private static double serviceAMinorGcTimeSec = 12.5;
    private static double serviceAMajorGcTimeSec = 3.2;

    private SkyWalkingJvmFixture() {
    }

    public static List<JVMMetricCollection> nextCollections() {
        long now = Instant.now().toEpochMilli();
        serviceAMinorGcCount += 2;
        serviceAMinorGcTimeSec += 0.06;
        return List.of(
                JVMMetricCollection.newBuilder()
                        .setService(SkyWalkingTraceFixture.SERVICE_A)
                        .setServiceInstance("service-a-1")
                        .addMetrics(metricForServiceA(now))
                        .build(),
                JVMMetricCollection.newBuilder()
                        .setService(SkyWalkingTraceFixture.SERVICE_B)
                        .setServiceInstance("service-b-1")
                        .addMetrics(metricForServiceB(now))
                        .build());
    }

    private static JVMMetric metricForServiceA(long timeMs) {
        return JVMMetric.newBuilder()
                .setTime(timeMs)
                .setCpu(CPU.newBuilder().setUsagePercent(15.0))
                .addMemory(heapMemory(256L * 1024 * 1024, 512L * 1024 * 1024, 268435456L))
                .addMemory(nonHeapMemory(64L * 1024 * 1024, 256L * 1024 * 1024, 96L * 1024 * 1024))
                .addMemoryPool(MemoryPool.newBuilder().setType(PoolType.NEWGEN_USAGE).setUsed(100L * 1024 * 1024))
                .addMemoryPool(MemoryPool.newBuilder().setType(PoolType.OLDGEN_USAGE).setUsed(150L * 1024 * 1024))
                .addGc(GC.newBuilder().setPhase(GCPhase.NEW).setCount(serviceAMinorGcCount)
                        .setTime((long) (serviceAMinorGcTimeSec * 1000)))
                .addGc(GC.newBuilder().setPhase(GCPhase.OLD).setCount(serviceAMajorGcCount)
                        .setTime((long) (serviceAMajorGcTimeSec * 1000)))
                .setThread(Thread.newBuilder().setLiveCount(28))
                .setClazz(Class.newBuilder().setLoadedClassCount(8456))
                .build();
    }

    private static JVMMetric metricForServiceB(long timeMs) {
        return JVMMetric.newBuilder()
                .setTime(timeMs)
                .setCpu(CPU.newBuilder().setUsagePercent(12.0))
                .addMemory(heapMemory(256L * 1024 * 1024, 512L * 1024 * 1024, 251658240L))
                .addMemory(nonHeapMemory(64L * 1024 * 1024, 256L * 1024 * 1024, 88L * 1024 * 1024))
                .addGc(GC.newBuilder().setPhase(GCPhase.NEW).setCount(95).setTime(8100))
                .setThread(Thread.newBuilder().setLiveCount(24))
                .setClazz(Class.newBuilder().setLoadedClassCount(8312))
                .build();
    }

    private static Memory heapMemory(long init, long max, long used) {
        return Memory.newBuilder()
                .setIsHeap(true)
                .setInit(init)
                .setMax(max)
                .setUsed(used)
                .setCommitted(used + 16L * 1024 * 1024)
                .build();
    }

    private static Memory nonHeapMemory(long init, long max, long used) {
        return Memory.newBuilder()
                .setIsHeap(false)
                .setInit(init)
                .setMax(max)
                .setUsed(used)
                .setCommitted(used + 8L * 1024 * 1024)
                .build();
    }
}
