package com.databuff.apm.web.ai.platform.tool;

import com.databuff.apm.web.ai.TestBeanSupport;
import com.databuff.apm.web.ai.platform.expert.AiExpertDefinition;
import com.databuff.apm.web.ai.platform.expert.ExpertRuntimeOptions;
import com.databuff.apm.web.ai.platform.expert.ExpertToolAccessMode;
import com.databuff.apm.web.ai.platform.expert.ExpertType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpertToolResolverTest {

    @Test
    void allowlistOnlyRegistersSelectedTools() {
        ToolManagementService tools = TestBeanSupport.toolManagementService();
        ExpertToolResolver resolver = new ExpertToolResolver(tools);
        AiExpertDefinition expert = expert(List.of("data.queryServicesAll"), ExpertToolAccessMode.ALLOWLIST);

        List<String> resolved = resolver.resolve(expert).stream().map(AiToolDefinition::toolId).toList();

        assertThat(resolved).containsExactly("data.queryServicesAll");
    }

    @Test
    void blocklistExcludesSelectedTools() {
        ToolManagementService tools = TestBeanSupport.toolManagementService();
        ExpertToolResolver resolver = new ExpertToolResolver(tools);
        AiExpertDefinition expert = expert(
                List.of("data.queryServicesAll"),
                ExpertToolAccessMode.BLOCKLIST);

        List<String> resolved = resolver.resolve(expert).stream().map(AiToolDefinition::toolId).toList();

        assertThat(resolved).contains("common.getCurrentTimeRange", "data.queryServiceTopology", "inspect.inspectService");
        assertThat(resolved).doesNotContain("data.queryServicesAll");
    }

    private static AiExpertDefinition expert(List<String> toolIds, ExpertToolAccessMode mode) {
        Instant now = Instant.now();
        ExpertRuntimeOptions options = new ExpertRuntimeOptions(
                "默认分类", true, false, false, true, mode);
        return new AiExpertDefinition(
                "custom", "Custom", null, "desc", ExpertType.CUSTOM,
                null, null, "prompt", toolIds, List.of(),
                options, true, false, 1L, now, now);
    }
}
