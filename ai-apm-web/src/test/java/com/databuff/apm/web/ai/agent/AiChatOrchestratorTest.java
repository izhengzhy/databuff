package com.databuff.apm.web.ai.agent;

import com.databuff.apm.web.ai.TestAiSupport;
import com.databuff.apm.web.ai.TestBeanSupport;
import com.databuff.apm.web.ai.OpenAiCompatibleChatClient;
import com.databuff.apm.web.ai.platform.task.ExpertMessageConstants;
import com.databuff.apm.web.ai.platform.task.ExpertTask;
import com.databuff.apm.web.ai.platform.task.ExpertTaskContext;
import com.databuff.apm.web.ai.platform.task.ExpertTaskService;
import com.databuff.apm.web.ai.platform.task.ExpertTaskStatus;
import com.databuff.apm.web.support.WebTestClusterSupport;
import com.databuff.apm.web.ai.tool.ApmToolkit;
import io.agentscope.core.agent.RuntimeContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatOrchestratorTest {

    @Test
    void rejectsUnknownExpert() {
        AgentBrainService service = TestAiSupport.aiFixture().agentBrain(Mockito.mock(ApmToolkit.class), new AiSessionStore());

        assertThatThrownBy(() -> service.chat(new AgentBrainService.ChatRequest(
                null, "missing", "hello", false, java.util.Map.of(), null)))
                .isInstanceOf(AiPlatformChatException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void submitChatReturnsProcessingStatus() {
        AgentBrainService service = TestAiSupport.aiFixture().agentBrain(mock(ApmToolkit.class), new AiSessionStore());
        AgentBrainService.ChatSubmitResponse submitted = service.submitChat(
                new AgentBrainService.ChatRequest(null, "inspection", "hello", false, java.util.Map.of(), null));
        assertThat(submitted.status()).isEqualTo("PROCESSING");
        assertThat(submitted.assistantMessageId()).isNotBlank();
    }

    @Test
    void pollMessagesReflectRunningState() throws Exception {
        ApmToolkit toolkit = mock(ApmToolkit.class);
        when(toolkit.countRecentSpans(anyLong())).thenReturn(3);
        AgentBrainService service = TestAiSupport.aiFixture().agentBrain(toolkit, new AiSessionStore());
        AgentBrainService.ChatSubmitResponse submitted = service.submitChat(
                new AgentBrainService.ChatRequest(null, "最近 trace 有多少"));
        assertThat(submitted.status()).isEqualTo("PROCESSING");

        AiSessionStore.MessagePollResponse poll = null;
        for (int i = 0; i < 80; i++) {
            poll = service.pollMessages(submitted.sessionId(), null);
            if (!poll.running()) {
                break;
            }
            Thread.sleep(50L);
        }
        assertThat(poll).isNotNull();
        assertThat(poll.running()).isFalse();
        assertThat(poll.messages()).anyMatch(message -> message.messageId().equals(submitted.assistantMessageId()));
    }

    @Test
    void recordsExpertIdInSession() {
        AgentBrainService service = TestAiSupport.aiFixture().agentBrain(mock(ApmToolkit.class), new AiSessionStore());
        AgentBrainService.ChatResponse response = service.chat(new AgentBrainService.ChatRequest(
                null, "inspection", "hello", false, java.util.Map.of(), null));

        assertThat(response.expertId()).isEqualTo("inspection");
        assertThat(service.listSessions(0, 20).get("data")).asList().singleElement()
                .satisfies(summary -> assertThat(((AiSessionStore.SessionSummary) summary).expertId()).isEqualTo("inspection"));
    }

    @Test
    void persistsUserVisibleMessageAndKeepsAttachmentsOffAssistant() throws Exception {
        AiSessionStore store = new AiSessionStore();
        AgentBrainService service = TestAiSupport.aiFixture().agentBrain(mock(ApmToolkit.class), store);
        String pngBytes = "fake-png";
        String dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes.getBytes());

        AgentBrainService.ChatSubmitResponse submitted = service.submitChat(new AgentBrainService.ChatRequest(
                null,
                "inspection",
                "请看这张图",
                false,
                Map.of("attachments", List.of(Map.of(
                        "type", "image",
                        "name", "image.png",
                        "mimeType", "image/png",
                        "size", pngBytes.length(),
                        "dataUrl", dataUrl))),
                null));

        AiSessionStore.MessagePollResponse poll = null;
        for (int i = 0; i < 80; i++) {
            poll = service.pollMessages(submitted.sessionId(), null);
            if (!poll.running()) {
                break;
            }
            Thread.sleep(50L);
        }
        assertThat(poll).isNotNull();
        assertThat(poll.running()).isFalse();

        AiSessionStore.ChatMessage userMessage = poll.messages().stream()
                .filter(message -> "user".equals(message.role()))
                .findFirst()
                .orElseThrow();
        assertThat(userMessage.content()).isEqualTo("请看这张图");
        assertThat(userMessage.content()).doesNotContain("[Session workspace attachments]");
        assertThat(userMessage.metadata()).containsKey("attachments");

        AiSessionStore.ChatMessage assistantMessage = poll.messages().stream()
                .filter(message -> submitted.assistantMessageId().equals(message.messageId()))
                .findFirst()
                .orElseThrow();
        assertThat(assistantMessage.metadata()).doesNotContainKey("attachments");
    }

    @Test
    void brainChatStoresSubtaskMetadataWhenDispatchRuns() {
        TestAiSupport.AiFixture aiFixture = TestAiSupport.aiFixture();
        TestAiSupport.PlatformRuntimeFixture fixture =
                aiFixture.buildPlatformRuntime(mock(ApmToolkit.class));
        AiSessionStore store = new AiSessionStore();
        AiRuntimeRouter runtimeRouter = WebTestClusterSupport.standaloneAiRouter("web-1");
        AiChatOrchestrator orchestrator = TestBeanSupport.chatOrchestrator(
                fixture.expertManagementService(),
                fixture.expertRuntimeRegistry(),
                fixture.sessionExpertRuntimeRegistry(),
                store,
                aiFixture.aiConfigService(),
                aiFixture.agentRuntimeConfig(),
                mock(ApmToolkit.class),
                new OpenAiCompatibleChatClient(aiFixture.agentRuntimeConfig()),
                aiFixture.store(),
                runtimeRouter,
                new AiRuntimeForwarder(runtimeRouter, 120L),
                fixture.expertTaskService(),
                fixture.expertTaskPendingRegistry(),
                fixture.expertTaskTextGuard(),
                fixture.sessionWorkspaceService(),
                15);
        fixture.wireBrainContinuer(orchestrator);
        String sessionId = store.ensureSession(null, "brain", "rk", "web-1");
        ExpertTaskContext.run(sessionId, "brain", null, () -> {
            fixture.expertDispatchTool().dispatchExpertTask(
                    "data",
                    "metrics",
                    "{}",
                    RuntimeContext.builder().sessionId(sessionId).build());
            return null;
        });

        Map<String, Object> metadata = orchestrator.buildAssistantMetadata(sessionId, "brain");
        assertThat(metadata).containsKey("subtasks");
    }

    @Test
    void brainMetadataAggregatesCurrentRoundTaskGeneratedFiles() {
        TestAiSupport.AiFixture aiFixture = TestAiSupport.aiFixture();
        TestAiSupport.PlatformRuntimeFixture fixture =
                aiFixture.buildPlatformRuntime(mock(ApmToolkit.class));
        AiSessionStore store = new AiSessionStore();
        ExpertTaskService taskService = mock(ExpertTaskService.class);
        AiRuntimeRouter runtimeRouter = WebTestClusterSupport.standaloneAiRouter("web-1");
        AiChatOrchestrator orchestrator = TestBeanSupport.chatOrchestrator(
                fixture.expertManagementService(),
                fixture.expertRuntimeRegistry(),
                fixture.sessionExpertRuntimeRegistry(),
                store,
                aiFixture.aiConfigService(),
                aiFixture.agentRuntimeConfig(),
                mock(ApmToolkit.class),
                new OpenAiCompatibleChatClient(aiFixture.agentRuntimeConfig()),
                aiFixture.store(),
                runtimeRouter,
                new AiRuntimeForwarder(runtimeRouter, 120L),
                taskService,
                fixture.expertTaskPendingRegistry(),
                fixture.expertTaskTextGuard(),
                fixture.sessionWorkspaceService(),
                15);
        String sessionId = store.ensureSession(null, "brain", "rk", "web-1");
        Instant now = Instant.now();
        ExpertTask task = new ExpertTask(
                "task-report",
                null,
                sessionId,
                "brain",
                "inspection",
                ExpertTaskStatus.SUCCEEDED,
                "inspect",
                "done",
                null,
                Map.of(
                        ExpertMessageConstants.META_ROUND_INDEX, 1,
                        "generatedFiles", List.of(Map.of(
                                "type", "file",
                                "name", "service-b-inspection-report.html",
                                "filePath", "outputs/service-b-inspection-report.html",
                                "mimeType", "text/html",
                                "size", 1024L))),
                now,
                now,
                now);
        when(taskService.listBySession(sessionId)).thenReturn(List.of(task));

        Map<String, Object> metadata = orchestrator.buildAssistantMetadata(sessionId, "brain", Set.of());

        assertThat(metadata).containsKey("generatedFiles");
        assertThat((List<?>) metadata.get("generatedFiles"))
                .singleElement()
                .satisfies(item -> assertThat(((Map<?, ?>) item).get("filePath"))
                        .isEqualTo("outputs/service-b-inspection-report.html"));
    }
}
