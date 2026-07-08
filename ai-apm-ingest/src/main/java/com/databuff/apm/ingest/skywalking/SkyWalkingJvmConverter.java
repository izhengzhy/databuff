package com.databuff.apm.ingest.skywalking;

import com.databuff.apm.common.util.ServiceKeyUtil;
import com.databuff.apm.ingest.otel.OtlMetricLine;
import org.apache.skywalking.apm.network.language.agent.v3.GC;
import org.apache.skywalking.apm.network.language.agent.v3.GCPhase;
import org.apache.skywalking.apm.network.language.agent.v3.JVMMetric;
import org.apache.skywalking.apm.network.language.agent.v3.JVMMetricCollection;
import org.apache.skywalking.apm.network.language.agent.v3.Memory;
import org.apache.skywalking.apm.network.language.agent.v3.MemoryPool;
import org.apache.skywalking.apm.network.language.agent.v3.PoolType;

import java.util.ArrayList;
import java.util.List;

/** SkyWalking JVMMetricCollection → {@link OtlMetricLine} list. */
public final class SkyWalkingJvmConverter {

    public List<ConvertedMetric> convert(JVMMetricCollection collection) {
        if (collection == null || collection.getService().isBlank()) {
            return List.of();
        }
        String serviceName = collection.getService();
        String serviceKey = ServiceKeyUtil.of(serviceName);
        String serviceInstance = collection.getServiceInstance();
        List<ConvertedMetric> out = new ArrayList<>();
        for (JVMMetric metric : collection.getMetricsList()) {
            long tsMillis = metric.getTime() > 0 ? metric.getTime() : System.currentTimeMillis();
            if (metric.getCpu().getUsagePercent() > 0) {
                addLine(out, serviceKey, serviceName, serviceInstance, tsMillis,
                        "jvm.cpu_load_process", metric.getCpu().getUsagePercent() / 100.0);
            }
            for (Memory memory : metric.getMemoryList()) {
                String prefix = memory.getIsHeap() ? "jvm.memory.heap" : "jvm.memory.noheap";
                addLine(out, serviceKey, serviceName, serviceInstance, tsMillis, prefix + ".init", memory.getInit());
                addLine(out, serviceKey, serviceName, serviceInstance, tsMillis, prefix + ".max", memory.getMax());
                addLine(out, serviceKey, serviceName, serviceInstance, tsMillis, prefix + ".used", memory.getUsed());
                addLine(out, serviceKey, serviceName, serviceInstance, tsMillis, prefix + ".committed", memory.getCommitted());
            }
            for (MemoryPool pool : metric.getMemoryPoolList()) {
                String identifier = poolIdentifier(pool.getType());
                if (identifier != null && pool.getUsed() > 0) {
                    addLine(out, serviceKey, serviceName, serviceInstance, tsMillis, identifier, pool.getUsed());
                }
            }
            for (GC gc : metric.getGcList()) {
                if (gc.getPhase() == GCPhase.NEW) {
                    addLine(out, serviceKey, serviceName, serviceInstance, tsMillis,
                            "jvm.gc.minor_collection_count", gc.getCount());
                    addLine(out, serviceKey, serviceName, serviceInstance, tsMillis,
                            "jvm.gc.minor_collection_time", gc.getTime() / 1000.0);
                } else if (gc.getPhase() == GCPhase.OLD) {
                    addLine(out, serviceKey, serviceName, serviceInstance, tsMillis,
                            "jvm.gc.major_collection_count", gc.getCount());
                    addLine(out, serviceKey, serviceName, serviceInstance, tsMillis,
                            "jvm.gc.major_collection_time", gc.getTime() / 1000.0);
                }
            }
            if (metric.getThread().getLiveCount() > 0) {
                addLine(out, serviceKey, serviceName, serviceInstance, tsMillis,
                        "jvm.thread_count", metric.getThread().getLiveCount());
            }
            if (metric.getClazz().getLoadedClassCount() > 0) {
                addLine(out, serviceKey, serviceName, serviceInstance, tsMillis,
                        "jvm.loaded_classes.count", metric.getClazz().getLoadedClassCount());
            }
        }
        return out;
    }

    private static String poolIdentifier(PoolType type) {
        return switch (type) {
            case NEWGEN_USAGE -> "jvm.gc.eden_size";
            case OLDGEN_USAGE -> "jvm.gc.old_gen_size";
            case SURVIVOR_USAGE -> "jvm.gc.survivor_size";
            case METASPACE_USAGE, PERMGEN_USAGE -> "jvm.gc.metaspace_size";
            default -> null;
        };
    }

    private static void addLine(
            List<ConvertedMetric> out,
            String serviceKey,
            String serviceName,
            String serviceInstance,
            long tsMillis,
            String metric,
            Number value) {
        if (value == null) {
            return;
        }
        if (value instanceof Long longValue && longValue == 0 && metric.endsWith(".max")) {
            return;
        }
        out.add(new ConvertedMetric(serviceKey, new OtlMetricLine(
                tsMillis,
                serviceKey,
                serviceName,
                metric,
                value,
                serviceInstance,
                serviceInstance,
                null,
                null,
                null,
                null,
                null,
                null)));
    }

    public record ConvertedMetric(String serviceKey, OtlMetricLine line) {
    }
}
