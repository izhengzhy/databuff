package com.databuff.apm.web.portal;

import com.databuff.apm.common.storage.ApmReadRepository;
import com.databuff.apm.web.TestStorageSupport;
import com.databuff.apm.web.config.ApmStorageProperties;
import com.databuff.apm.web.flow.ServiceFlowService;
import com.databuff.apm.web.trace.TraceQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Integration-style test for {@code POST /webapi/trace/spans}:
 * trace detail spans expose {@code service_type}/{@code type} aligned with the
 * frontend legend (web/db/cache/mq/custom).
 */
@SpringJUnitConfig(classes = TracePortalSpanTypeIntegrationTest.Config.class)
@ActiveProfiles({"local", "test"})
class TracePortalSpanTypeIntegrationTest {

    @Autowired
    private TraceQueryService traceQueryService;

    @Autowired
    private TracePortalService tracePortalService;

    @BeforeEach
    void setUp() {
        reset(traceQueryService);
    }

    @Test
    void traceSpansResolveDemoCheckoutComponentServiceTypes() {
        when(traceQueryService.traceDetail(any()))
                .thenReturn(DemoCheckoutTraceDetailFixture.checkoutTraceDetails());

        Map<String, Object> response = tracePortalService.traceSpans(Map.of(
                "traceId", DemoCheckoutTraceDetailFixture.TRACE_ID,
                "size", 100));

        assertThat(response.get("status")).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> spans = (List<Map<String, Object>>) response.get("data");
        assertThat(spans).hasSize(13);

        Map<String, Map<String, Object>> spansById = spans.stream()
                .collect(Collectors.toMap(span -> String.valueOf(span.get("span_id")), Function.identity()));

        assertSpanType(spansById.get("root-a"), "web", "web");
        assertSpanType(spansById.get("http-mysql"), "db", "mysql");
        assertSpanType(spansById.get("redis-server-b"), "cache", "redis");
        assertSpanType(spansById.get("kafka-client"), "mq", "kafka");
        assertSpanType(spansById.get("dubbo-client"), "custom", "dubbo");
        assertSpanType(spansById.get("es-client"), "db", "elasticsearch");
        assertSpanType(spansById.get("http-client"), "web", "web");
        assertSpanType(spansById.get("remote-http"), "web", "web");

        Map<String, Object> errorDbSpan = spansById.get("dubbo-mysql");
        assertSpanType(errorDbSpan, "db", "mysql");
        assertThat(errorDbSpan.get("error")).isEqualTo(1);
    }

    private static void assertSpanType(Map<String, Object> span, String serviceType, String typeIcon) {
        assertThat(span.get("service_type")).isEqualTo(serviceType);
        assertThat(span.get("type")).isEqualTo(typeIcon);
    }

    @Configuration
    static class Config {

        @Bean
        ApmReadRepository apmReadRepository() {
            return mock(ApmReadRepository.class);
        }

        @Bean
        ApmStorageProperties apmStorageProperties() {
            return TestStorageSupport.storage();
        }

        @Bean
        TraceQueryService traceQueryService() {
            return mock(TraceQueryService.class);
        }

        @Bean
        ServiceFlowService serviceFlowService() {
            return mock(ServiceFlowService.class);
        }

        @Bean
        TracePortalService tracePortalService(
                TraceQueryService traceQueryService,
                ServiceFlowService serviceFlowService,
                ApmReadRepository apmReadRepository,
                ApmStorageProperties apmStorageProperties) {
            return new TracePortalService(
                    traceQueryService, serviceFlowService, apmReadRepository, apmStorageProperties);
        }
    }
}
