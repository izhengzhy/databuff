package com.databuff.apm.web.tools.local;

import com.databuff.apm.web.ai.platform.runtime.ExpertChatScopeRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledOnOs({OS.LINUX, OS.MAC})
class BashToolsTest {

    private final BashBackgroundTaskManager backgroundTaskManager = new BashBackgroundTaskManager();
    private final BashSessionManager sessionManager = new BashSessionManager(backgroundTaskManager);
    private final BashTools bashTools = new BashTools(sessionManager, backgroundTaskManager, new ObjectMapper());

    @AfterEach
    void tearDown() {
        ExpertChatScopeRegistry.clearForTests();
        sessionManager.closeSession("chat-1");
        sessionManager.closeSession("default");
    }

    @Test
    void runsCommandThroughBashTool() {
        ExpertChatScopeRegistry.register(new com.databuff.apm.web.ai.platform.runtime.ExpertChatContext.State(
                "chat-1", "tester", "ops", "msg-1", false, null, null));

        String result = bashTools.bash("echo databuff", "Echo test line", null, null);

        assertThat(result).contains("Exit code: 0");
        assertThat(result).contains("databuff");
    }

    @Test
    void runsBackgroundCommandAndReadsOutput() throws Exception {
        ExpertChatScopeRegistry.register(new com.databuff.apm.web.ai.platform.runtime.ExpertChatContext.State(
                "chat-1", "tester", "ops", "msg-1", false, null, null));

        String started = bashTools.bash("bash -lc 'echo line1; sleep 0.2; echo line2'", "Run background script", null, true);
        assertThat(started).contains("bash_id:");

        String bashId = started.substring(started.indexOf("bash_id:") + "bash_id:".length()).trim();

        StringBuilder allOutput = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            String chunk = bashTools.bashOutput(bashId, null);
            allOutput.append(chunk).append('\n');
            if (chunk.contains("status: completed")) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(allOutput).contains("line1");
        assertThat(allOutput).contains("line2");
        assertThat(allOutput).contains("status: completed");
        assertThat(allOutput).contains("exit code: 0");
    }

    @Test
    void killsBackgroundShell() {
        ExpertChatScopeRegistry.register(new com.databuff.apm.web.ai.platform.runtime.ExpertChatContext.State(
                "chat-1", "tester", "ops", "msg-1", false, null, null));

        String started = bashTools.bash("sleep 30", "Sleep in background", null, true);
        String bashId = started.substring(started.indexOf("bash_id:") + "bash_id:".length()).trim();

        String killed = bashTools.killShell(bashId);
        assertThat(killed).contains("killed successfully");
    }

    @Test
    void requiresCommand() {
        String result = bashTools.bash("  ", null, null, null);
        assertThat(result).contains("command is required");
    }

    @Test
    void rejectsRmCommand() {
        String result = bashTools.bash("rm -rf /tmp/foo", "Remove temp dir", null, null);
        assertThat(result).contains("Command rejected: rm is not allowed");
    }

    @Test
    void rejectsRmInChainedCommand() {
        String result = bashTools.bash("echo ok && rm file.txt", "Chain with rm", null, null);
        assertThat(result).contains("Command rejected: rm is not allowed");
    }

    @Test
    void rejectsRmInBackgroundCommand() {
        String result = bashTools.bash("rm -rf /tmp/foo", "Remove in background", null, true);
        assertThat(result).contains("Command rejected: rm is not allowed");
    }

    @Test
    void allowsCommandWithoutRmSpace() {
        ExpertChatScopeRegistry.register(new com.databuff.apm.web.ai.platform.runtime.ExpertChatContext.State(
                "chat-1", "tester", "ops", "msg-1", false, null, null));

        String result = bashTools.bash("echo harmless", "Echo harmless", null, null);
        assertThat(result).contains("Exit code: 0");
        assertThat(result).contains("harmless");
    }
}
