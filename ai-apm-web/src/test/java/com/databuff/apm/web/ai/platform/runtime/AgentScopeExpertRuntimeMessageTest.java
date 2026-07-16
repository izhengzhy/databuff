package com.databuff.apm.web.ai.platform.runtime;

import com.databuff.apm.web.ai.agent.AgentRuntimeConfig;
import com.databuff.apm.web.ai.agent.AiSessionStore;
import com.databuff.apm.web.ai.platform.expert.AiExpertDefinition;
import com.databuff.apm.web.ai.platform.expert.ExpertRuntimeOptions;
import com.databuff.apm.web.ai.platform.expert.ExpertType;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AgentScopeExpertRuntimeMessageTest {

    private static final byte[] TINY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @TempDir
    Path tempDir;

    private SessionWorkspaceService workspaceService;
    private AgentScopeExpertRuntime runtime;

    @BeforeEach
    void setUp() throws Exception {
        AgentRuntimeConfig config = new AgentRuntimeConfig();
        config.setWorkspaceDir(tempDir.toString());
        workspaceService = new SessionWorkspaceService(config);
        runtime = new AgentScopeExpertRuntime(
                new AiExpertDefinition(
                        "inspection",
                        "Inspection",
                        "默认分类",
                        "",
                        ExpertType.SPECIALIST,
                        null,
                        null,
                        "",
                        List.of(),
                        List.of(),
                        ExpertRuntimeOptions.defaults(),
                        true,
                        true,
                        1L,
                        Instant.now(),
                        Instant.now()),
                mock(ReActAgent.class),
                new RuntimeCacheKey("inspection", 1L, "", "", "deepseek", ""),
                Instant.now(),
                new AgentScopeSessionHook(new AiSessionStore()),
                workspaceService,
                List.of());
    }

    @Test
    void buildUserMessageIncludesImageBlocksForUploadedAttachments() throws Exception {
        String sessionId = "session-test-runtime-001";
        Files.createDirectories(workspaceService.uploadsDir(sessionId));
        Files.write(workspaceService.uploadsDir(sessionId).resolve("photo.png"), TINY_PNG);

        ExpertChatInput input = new ExpertChatInput(
                "请描述图片",
                sessionId,
                "admin",
                "assistant-1",
                Map.of("attachments", List.of(Map.of(
                        "type", "image",
                        "name", "photo.png",
                        "mimeType", "image/png",
                        "filePath", "uploads/photo.png"))));

        Msg message = invokeBuildUserMessage(input);

        assertThat(message.getContent()).hasSize(2);
        assertThat(message.getContent().get(0)).isInstanceOf(TextBlock.class);
        assertThat(((TextBlock) message.getContent().get(0)).getText()).isEqualTo("请描述图片");
        assertThat(message.getContent().get(1)).isInstanceOf(ImageBlock.class);
    }

    private Msg invokeBuildUserMessage(ExpertChatInput input) throws Exception {
        Method method = AgentScopeExpertRuntime.class.getDeclaredMethod("buildUserMessage", ExpertChatInput.class);
        method.setAccessible(true);
        return (Msg) method.invoke(runtime, input);
    }
}
