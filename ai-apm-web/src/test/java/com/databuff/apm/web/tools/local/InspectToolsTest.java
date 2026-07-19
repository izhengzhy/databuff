package com.databuff.apm.web.tools.local;

import com.databuff.apm.web.monitor.Alarm;
import com.databuff.apm.web.monitor.AlarmStore;
import com.databuff.apm.web.monitor.service.AlarmService;
import com.databuff.apm.web.portal.LogPortalService;
import com.databuff.apm.web.portal.ServicePortalService;
import com.databuff.apm.web.portal.TracePortalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectToolsTest {

    @Mock
    private ServicePortalService servicePortalService;
    @Mock
    private LogPortalService logPortalService;
    @Mock
    private TracePortalService tracePortalService;
    @Mock
    private AlarmService alarmService;
    @Mock
    private AlarmStore alarmStore;

    private InspectTools inspectTools;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        inspectTools = new InspectTools();
        ReflectionTestUtils.setField(inspectTools, "servicePortalService", servicePortalService);
        ReflectionTestUtils.setField(inspectTools, "logPortalService", logPortalService);
        ReflectionTestUtils.setField(inspectTools, "tracePortalService", tracePortalService);
        ReflectionTestUtils.setField(inspectTools, "alarmService", alarmService);
        ReflectionTestUtils.setField(inspectTools, "alarmStore", alarmStore);
        ReflectionTestUtils.setField(inspectTools, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(inspectTools, "metricDatabase", "databuff");
    }

    @AfterEach
    void tearDown() {
        inspectTools.shutdownInspectionExecutor();
    }

    @Test
    void detectAnomalyFindsLatestSpikeWithoutThreshold() {
        Map<String, Object> detection = InspectTools.detectAnomaly(List.of(10.0, 11.0, 9.0, 50.0));

        assertThat(detection).containsEntry("anomaly", true)
                .containsEntry("reason", "最新点相对历史均值存在明显突增");
    }

    @Test
    void detectAnomalyRequiresEnoughPoints() {
        Map<String, Object> detection = InspectTools.detectAnomaly(List.of(10.0, 50.0));

        assertThat(detection).containsEntry("anomaly", false)
                .containsEntry("reason", "数据点不足")
                .containsEntry("points", 2);
    }

    @Test
    void inspectServiceIncludesDeepDiveSignals() throws Exception {
        when(servicePortalService.serviceInfo(any())).thenReturn(Map.of(
                "service", "order-api",
                "name", "order-api",
                "serviceId", "order-api",
                "service_type", "db",
                "type", "db"));
        when(servicePortalService.serviceDetailTrendChart(any())).thenReturn(List.of());
        when(servicePortalService.getServiceInstanceRelations(any())).thenReturn(Map.of(
                "downflowServiceStats", List.of(Map.of(
                        "serviceId", "db-order",
                        "reqOutCnt", 100L,
                        "reqOutErrCnt", 20L,
                        "reqInCnt", 0L,
                        "reqInErrCnt", 0L,
                        "componentType", "service.db")),
                "upflowServiceStats", List.of(),
                "serviceId2Name", List.of(Map.of("serviceId", "db-order", "name", "order-db"))));
        when(servicePortalService.getServiceInstance(any(), any())).thenReturn(
                List.of(Map.of("serviceInstance", "pod-a", "hostName", "h1", "serviceCall", 10)),
                List.of(
                        Map.of("serviceInstance", "pod-a", "hostName", "h1", "serviceCall", 10),
                        Map.of("serviceInstance", "pod-b", "hostName", "h2", "serviceCall", 8)));
        when(logPortalService.trend(any())).thenReturn(Map.of(
                "data", Map.of(
                        "severityCnts", errorSpikeSeries(),
                        "logCnts", Map.of())));
        when(logPortalService.search(any())).thenAnswer(invocation -> {
            Map<String, Object> body = invocation.getArgument(0);
            if (body.get("query") != null) {
                return Map.of(
                        "total", 3L,
                        "data", List.of(Map.of(
                                "timestamp", "1",
                                "severity", "ERROR",
                                "message", "timeout talking to db",
                                "traceId", "t-kw")));
            }
            return Map.of(
                    "total", 1L,
                    "data", List.of(Map.of(
                            "timestamp", "1",
                            "severity", "ERROR",
                            "message", "boom",
                            "traceId", "t1")));
        });
        when(tracePortalService.errorSpanList(any())).thenReturn(Map.of(
                "data", Map.of(
                        "total", 5L,
                        "list", List.of(Map.of(
                                "traceId", "trace-err",
                                "resource", "/checkout",
                                "errorType", "Timeout",
                                "duration", 1200)))));
        when(alarmService.list(any())).thenReturn(Map.of(
                "data", Map.of(
                        "total", 2L,
                        "list", List.of(Map.of(
                                "id", "a1",
                                "description", "error rate high",
                                "level", 3,
                                "ruleName", "err-rate")))));
        when(alarmStore.listOpen()).thenReturn(List.of(
                new Alarm("open-1", 0L, "order-api", "threshold", "critical", "still firing",
                        Alarm.STATUS_OPEN, Instant.now(), null)));

        String output = inspectTools.inspectService("order-api");
        JsonNode root = objectMapper.readTree(output);

        assertThat(root.path("ok").asBoolean()).isTrue();
        assertThat(root.path("logMetrics").path("errorLogs").path("detection").path("anomaly").asBoolean())
                .isTrue();
        assertThat(root.path("logKeywordMetrics").path("detection").path("anomaly").asBoolean()).isTrue();
        assertThat(root.path("alarmMetrics").path("openCount").asLong()).isEqualTo(1L);
        assertThat(root.path("dependencyMetrics").path("detection").path("anomaly").asBoolean()).isTrue();
        assertThat(root.path("dependencyMetrics").path("suspicious")).isNotEmpty();
        assertThat(root.path("traceMetrics").path("total").asLong()).isEqualTo(5L);
        assertThat(root.path("instanceMetrics").path("detection").path("anomaly").asBoolean()).isTrue();
        assertThat(root.path("summary").asText())
                .contains("ERROR 日志量")
                .contains("服务告警")
                .contains("上下游依赖")
                .contains("失败 Trace")
                .contains("服务实例");
        verify(logPortalService, times(1)).trend(any());
        verify(servicePortalService, times(1)).serviceInfo(any());
    }

    private static Map<String, Object> errorSpikeSeries() {
        Map<String, Object> series = new LinkedHashMap<>();
        series.put("1000", Map.of("ERROR", 1L, "INFO", 10L));
        series.put("2000", Map.of("ERROR", 1L, "INFO", 10L));
        series.put("3000", Map.of("ERROR", 2L, "INFO", 10L));
        series.put("4000", Map.of("ERROR", 40L, "INFO", 10L));
        return series;
    }
}
