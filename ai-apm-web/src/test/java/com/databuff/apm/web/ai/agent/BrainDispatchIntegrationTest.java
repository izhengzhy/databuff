package com.databuff.apm.web.ai.agent;

import com.databuff.apm.web.ai.TestAiSupport;
import com.databuff.apm.web.ai.platform.task.ExpertTaskContext;
import com.databuff.apm.web.ai.platform.task.ExpertTaskEvent;
import com.databuff.apm.web.ai.tool.ApmToolkit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BrainDispatchIntegrationTest {

    @Test
    void brainSessionCollectsSubtaskEvents() {
        TestAiSupport.PlatformRuntimeFixture fixture =
                TestAiSupport.aiFixture().buildPlatformRuntime(Mockito.mock(ApmToolkit.class));
        List<ExpertTaskEvent> events = new ArrayList<>();

        ExpertTaskContext.run("s-brain", "brain", events::add, () -> {
            String contextJson = "{\"sessionId\":\"s-brain\"}";
            fixture.expertDispatchTool().dispatchExpertTask("data", "health summary", contextJson, null);
            fixture.expertDispatchTool().dispatchExpertTask("inspection", "triage alerts", contextJson, null);
            return null;
        });

        assertThat(events).hasSizeGreaterThanOrEqualTo(2);
        assertThat(events.stream().map(ExpertTaskEvent::type))
                .contains("subtask.created");
        assertThat(fixture.expertTaskService().listBySession("s-brain")).hasSize(2);
    }
}
