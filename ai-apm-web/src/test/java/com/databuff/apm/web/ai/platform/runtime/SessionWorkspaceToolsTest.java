package com.databuff.apm.web.ai.platform.runtime;

import com.databuff.apm.web.ai.agent.AgentRuntimeConfig;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SessionWorkspaceToolsTest {

    private static final byte[] TINY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @TempDir
    Path tempDir;

    private SessionWorkspaceService workspaceService;
    private SessionWorkspaceTools tools;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        AgentRuntimeConfig config = new AgentRuntimeConfig();
        config.setWorkspaceDir(tempDir.toString());
        workspaceService = new SessionWorkspaceService(config);
        tools = new SessionWorkspaceTools(workspaceService, config);
        sessionId = "session-test-tools-001";
        Files.createDirectories(workspaceService.uploadsDir(sessionId));
        ExpertChatScopeRegistry.register(new ExpertChatContext.State(
                sessionId, "admin", "inspection", null, false, null));
    }

    @AfterEach
    void tearDown() {
        ExpertChatScopeRegistry.unregister(sessionId);
    }

    @Test
    void readWorkspaceFileReturnsImageBlockForPng() throws Exception {
        Files.write(workspaceService.uploadsDir(sessionId).resolve("chart.png"), TINY_PNG);

        ToolResultBlock result = tools.readWorkspaceFile("uploads/chart.png", null);

        assertThat(result.getOutput()).hasSize(2);
        assertThat(result.getOutput().get(0)).isInstanceOf(TextBlock.class);
        assertThat(((TextBlock) result.getOutput().get(0)).getText()).contains("uploads/chart.png");
        assertThat(result.getOutput().get(1)).isInstanceOf(ImageBlock.class);
    }

    @Test
    void readWorkspaceFileReturnsTextForCsv() throws Exception {
        Files.writeString(workspaceService.uploadsDir(sessionId).resolve("metrics.csv"), "a,b\n1,2");

        ToolResultBlock result = tools.readWorkspaceFile("uploads/metrics.csv", null);

        assertThat(result.getOutput()).hasSize(1);
        assertThat(result.getOutput().get(0)).isInstanceOf(TextBlock.class);
        assertThat(((TextBlock) result.getOutput().get(0)).getText()).contains("1: a,b");
    }
}
