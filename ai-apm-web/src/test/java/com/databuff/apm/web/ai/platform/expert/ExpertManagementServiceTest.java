package com.databuff.apm.web.ai.platform.expert;

import com.databuff.apm.web.ai.TestBeanSupport;
import com.databuff.apm.web.ai.platform.runtime.ExpertRuntimeRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.function.Consumer;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpertManagementServiceTest {

    @Test
    void seedsBuiltInExpertsAndProtectsDelete() {
        ExpertManagementService service = service();
        assertThat(service.list()).extracting(AiExpertDefinition::expertId)
                .contains("brain", "data", "inspection", "ops");
        assertThat(service.find("data")).get()
                .extracting(AiExpertDefinition::name)
                .isEqualTo("智能问数");
        assertThat(service.find("brain")).get()
                .extracting(AiExpertDefinition::name)
                .isEqualTo("AI大脑");
        assertThat(service.find("brain").orElseThrow().toolIds())
                .containsExactly("brain.dispatchExpertTask");
        assertThat(service.find("data").orElseThrow().toolIds())
                .contains("common.getCurrentTimeRange", "common.drawTrendCharts",
                        "data.queryServicesAll", "data.queryTraceDetail", "data.queryServiceAlarms");
        assertThat(service.find("inspection").orElseThrow().toolIds())
                .contains("common.getCurrentTimeRange", "data.queryMetricData", "inspect.inspectService");
        assertThat(service.find("ops").orElseThrow().toolIds())
                .contains("Bash", "BashOutput", "KillShell", "inspect.inspectService");
        assertThat(service.delete("brain")).isFalse();
    }

    @Test
    void savesCustomExpertWhenReferencesExist() {
        ExpertManagementService service = service();
        Instant now = Instant.now();
        AiExpertDefinition created = service.save(new AiExpertDefinition(
                "custom", "Custom", null, "custom expert", ExpertType.CUSTOM, null, null,
                "prompt", List.of("data.queryServicesAll"), List.of("skill.data.metrics"),
                ExpertRuntimeOptions.defaults(), true, false, 0, now, now));

        assertThat(created.version()).isEqualTo(1);
        assertThat(service.find("custom")).isPresent();
        assertThat(service.delete("custom")).isTrue();
    }

    @Test
    void rejectsMissingToolOrSkillReference() {
        ExpertManagementService service = service();
        Instant now = Instant.now();
        assertThatThrownBy(() -> service.save(new AiExpertDefinition(
                "custom", "Custom", null, "custom expert", ExpertType.CUSTOM, null, null,
                "prompt", List.of("missing.tool"), List.of("skill.data.metrics"),
                ExpertRuntimeOptions.defaults(), true, false, 0, now, now)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing.tool");

        assertThatThrownBy(() -> service.save(new AiExpertDefinition(
                "custom", "Custom", null, "custom expert", ExpertType.CUSTOM, null, null,
                "prompt", List.of("data.queryServicesAll"), List.of("missing.skill"),
                ExpertRuntimeOptions.defaults(), true, false, 0, now, now)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing.skill");
    }

    @Test
    void persistedBuiltInDataExpertIgnoresUnrelatedBuiltInSkills() {
        ExpertManagementService service = service();
        AiExpertDefinition data = service.find("data").orElseThrow();
        service.applyPersistedRows(List.of(new AiExpertDefinition(
                data.expertId(),
                data.name(),
                data.category(),
                data.description(),
                data.type(),
                data.modelProviderCode(),
                data.modelName(),
                data.systemPrompt(),
                data.toolIds(),
                List.of("skill.brain.routing", "skill.data.metrics", "skill.inspection.health"),
                data.options(),
                data.enabled(),
                true,
                data.version(),
                data.createdAt(),
                data.updatedAt())));

        assertThat(service.find("data").orElseThrow().skillIds())
                .containsExactly("skill.data.metrics");
    }

    @Test
    void persistedBuiltInDataExpertKeepsNewDefaultTools() {
        ExpertManagementService service = service();
        AiExpertDefinition data = service.find("data").orElseThrow();
        service.applyPersistedRows(List.of(new AiExpertDefinition(
                data.expertId(),
                "问数",
                data.category(),
                data.description(),
                data.type(),
                data.modelProviderCode(),
                data.modelName(),
                "You are the DataBuff APM data expert. Prefer APM tools for metrics and traces.",
                List.of("time.getCurrentTimeRange"),
                List.of("skill.data.metrics"),
                data.options(),
                data.enabled(),
                true,
                data.version(),
                data.createdAt(),
                data.updatedAt())));

        assertThat(service.find("data").orElseThrow().toolIds())
                .contains("time.getCurrentTimeRange", "common.getCurrentTimeRange",
                        "common.drawTrendCharts", "data.queryServicesAll", "data.queryTraceDetail");
        assertThat(service.find("data").orElseThrow().systemPrompt())
                .contains("智能问数")
                .contains("skill.data.metrics");
    }

    @Test
    void savingCustomExpertInvalidatesBrainRuntime() {
        ExpertRuntimeRegistry registry = org.mockito.Mockito.mock(ExpertRuntimeRegistry.class);
        ObjectProvider<ExpertRuntimeRegistry> registryProvider = new ObjectProvider<>() {
            @Override
            public ExpertRuntimeRegistry getObject() {
                return registry;
            }

            @Override
            public ExpertRuntimeRegistry getObject(Object... args) {
                return registry;
            }

            @Override
            public ExpertRuntimeRegistry getIfAvailable() {
                return registry;
            }

            @Override
            public ExpertRuntimeRegistry getIfUnique() {
                return registry;
            }

            @Override
            public void ifAvailable(Consumer<ExpertRuntimeRegistry> consumer) {
                consumer.accept(registry);
            }
        };
        ExpertManagementService service = TestBeanSupport.expertManagementService(
                TestBeanSupport.toolManagementService(),
                TestBeanSupport.skillManagementService(),
                null,
                registryProvider);
        Instant now = Instant.now();
        service.save(new AiExpertDefinition(
                "custom", "Custom", null, "custom expert", ExpertType.CUSTOM, null, null,
                "prompt", List.of("data.queryServicesAll"), List.of("skill.data.metrics"),
                ExpertRuntimeOptions.defaults(), true, false, 0, now, now));

        org.mockito.Mockito.verify(registry).invalidate(
                org.mockito.ArgumentMatchers.eq("brain"),
                org.mockito.ArgumentMatchers.contains("custom"));
    }

    @Test
    void persistedBuiltInBrainExpertUsesCatalogRoutingToolsOnly() {
        ExpertManagementService service = service();
        AiExpertDefinition brain = service.find("brain").orElseThrow();
        service.applyPersistedRows(List.of(new AiExpertDefinition(
                brain.expertId(),
                "大脑",
                brain.category(),
                brain.description(),
                brain.type(),
                brain.modelProviderCode(),
                brain.modelName(),
                brain.systemPrompt(),
                List.of("data.queryMetricData", "common.getCurrentTimeRange"),
                brain.skillIds(),
                brain.options(),
                brain.enabled(),
                true,
                brain.version(),
                brain.createdAt(),
                brain.updatedAt())));

        AiExpertDefinition merged = service.find("brain").orElseThrow();
        assertThat(merged.name()).isEqualTo("AI大脑");
        assertThat(merged.toolIds())
                .containsExactly("brain.dispatchExpertTask");
    }

    private static ExpertManagementService service() {
        return TestBeanSupport.expertManagementService(
                TestBeanSupport.toolManagementService(),
                TestBeanSupport.skillManagementService());
    }
}
