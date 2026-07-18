package com.databuff.apm.web.ai.platform.capability;

import com.databuff.apm.web.ai.TestBeanSupport;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilityManagementServiceTest {

    @Test
    void listsCapabilitiesByOrderedId() {
        CapabilityManagementService service = TestBeanSupport.capabilityManagementService();
        service.applyPersistedRows(List.of(definition("7"), definition("2"), definition("1")));

        assertThat(service.list())
                .extracting(AiCapabilityDefinition::capabilityId)
                .containsExactly("1", "2", "7");
    }

    private static AiCapabilityDefinition definition(String id) {
        Instant now = Instant.parse("2026-07-18T00:00:00Z");
        return new AiCapabilityDefinition(
                id, "capability-" + id, "tagline", null, "data", List.of(),
                true, true, 1L, now, now,
                "capability-" + id, "tagline", "data", List.of());
    }
}
