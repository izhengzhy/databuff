package com.databuff.apm.web.tools.local;

import com.databuff.apm.web.portal.LogPortalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogToolsTest {

    @Mock
    private LogPortalService logPortalService;

    private LogTools logTools;

    @BeforeEach
    void setUp() {
        logTools = new LogTools();
        ReflectionTestUtils.setField(logTools, "logPortalService", logPortalService);
        ReflectionTestUtils.setField(logTools, "objectMapper", new ObjectMapper());
    }

    @Test
    void queryLogDetailRequiresTimeRange() {
        String output = logTools.queryLogDetail(
                null, null, List.of("order-api"), null, null, null, null, null, null);
        assertThat(output).contains("\"ok\":false");
        assertThat(output).contains("fromTime");
    }

    @Test
    void queryLogsByTraceIdUsesTraceFilter() {
        when(logPortalService.search(any())).thenReturn(searchResponse());

        String output = logTools.queryLogsByTraceId(
                "trace-abc", List.of("ERROR"), "timeout", null, null, 0, 10);

        assertThat(output).contains("\"traceId\"");
        assertThat(output).contains("order created");
        verify(logPortalService).search(org.mockito.ArgumentMatchers.argThat(body ->
                "trace-abc".equals(body.get("traceId"))
                        && List.of("ERROR").equals(body.get("severities"))
                        && "timeout".equals(body.get("query"))));
    }

    @Test
    void queryLogsBySpanIdUsesSpanFilter() {
        when(logPortalService.search(any())).thenReturn(searchResponse());

        logTools.queryLogsBySpanId(
                "span-1", "trace-abc", null, null, null, null, null, null);

        verify(logPortalService).search(org.mockito.ArgumentMatchers.argThat(body ->
                "span-1".equals(body.get("spanId")) && "trace-abc".equals(body.get("traceId"))));
    }

    @Test
    void queryLogTrendDelegatesToPortalTrend() {
        Map<String, Object> trendData = Map.of(
                "logCnts", Map.of("1", 3),
                "severityCnts", Map.of());
        when(logPortalService.trend(any())).thenReturn(Map.of("data", trendData));

        String output = logTools.queryLogTrend(
                "2026-07-04 09:00:00",
                "2026-07-04 10:00:00",
                List.of("order-api"),
                null,
                List.of("pod-1"),
                List.of("ERROR"),
                "timeout",
                60);

        assertThat(output).contains("\"interval\":60");
        assertThat(output).contains("logCnts");
        verify(logPortalService).trend(org.mockito.ArgumentMatchers.argThat(body ->
                List.of("order-api").equals(body.get("services"))
                        && List.of("pod-1").equals(body.get("serviceInstances"))
                        && Integer.valueOf(60).equals(body.get("interval"))));
    }

    private static Map<String, Object> searchResponse() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("timestamp", "1718000123456");
        row.put("message", "order created");
        row.put("traceId", "trace-abc");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", 1L);
        response.put("offset", 0);
        response.put("data", List.of(row));
        return response;
    }
}
