package com.databuff.apm.web.tools.local;

import com.databuff.apm.web.ai.agent.AgentRuntimeConfig;
import com.databuff.apm.web.ai.agent.ShellCommandPolicy;
import com.databuff.apm.web.ai.platform.task.ExpertSessionResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Lazy
public class BashTools {

    static final long DEFAULT_TIMEOUT_MS = 120_000L;
    static final long MAX_TIMEOUT_MS = 600_000L;
    static final int MAX_OUTPUT_CHARS = 30_000;

    private static final String TOOL_DESCRIPTION = """
            Executes a given bash command in a persistent shell session with optional timeout, ensuring proper handling and security measures.

            Usage notes:
              - The command argument is required.
              - You can specify an optional timeout in milliseconds (up to 600000ms / 10 minutes). If not specified, commands will timeout after 120000ms (2 minutes).
              - It is very helpful if you write a clear, concise description of what this command does in 5-10 words.
              - If the output exceeds 30000 characters, output will be truncated before being returned to you.
              - You can use the run_in_background parameter to run the command in the background, which allows you to continue working while the command runs. You do not need to use '&' at the end of the command when using this parameter.
              - Never use run_in_background to run 'sleep' as it will return immediately.
              - When issuing multiple commands:
                - If the commands are independent and can run in parallel, make multiple Bash tool calls in a single message.
                - If the commands depend on each other and must run sequentially, use a single Bash call with '&&' to chain them together.
                - Use ';' only when you need to run commands sequentially but don't care if earlier commands fail.
                - DO NOT use newlines to separate commands (newlines are ok in quoted strings).
              - Try to maintain your current working directory throughout the session by using absolute paths and avoiding usage of cd. You may use cd if the User explicitly requests it.
              - Always quote file paths that contain spaces with double quotes.
              - For remote hosts, embed ssh or sshpass in the command, e.g. sshpass -p 'pwd' ssh user@host "df -h"
              - Remote ssh/sshpass commands automatically disable GSSAPI and use at least 60000ms timeout.
            """;

    private static final String BASH_OUTPUT_DESCRIPTION = """
            Retrieves output from a running or completed background bash shell.
            Takes a bash_id parameter identifying the shell.
            Always returns only new output since the last check.
            Returns stdout and stderr output along with shell status.
            Supports optional regex filtering to show only lines matching a pattern.
            Use this tool when you need to monitor or check the output of a long-running shell.
            """;

    private static final String KILL_SHELL_DESCRIPTION = """
            Kills a running background bash shell by its ID.
            Takes a shell_id parameter identifying the shell to kill.
            Returns a success or failure status.
            Use this tool when you need to terminate a long-running shell.
            """;

    private static final String DESCRIPTION_PARAM = """
            Clear, concise description of what this command does in 5-10 words, in active voice. Examples:
            Input: ls
            Output: List files in current directory

            Input: git status
            Output: Show working tree status

            Input: npm install
            Output: Install package dependencies

            Input: mkdir foo
            Output: Create directory 'foo'
            """;

    private final BashSessionManager sessionManager;
    private final BashBackgroundTaskManager backgroundTaskManager;
    private final ObjectMapper objectMapper;
    private final ShellCommandPolicy shellPolicy;

    public BashTools(
            BashSessionManager sessionManager,
            BashBackgroundTaskManager backgroundTaskManager,
            ObjectMapper objectMapper,
            AgentRuntimeConfig agentRuntimeConfig) {
        this.sessionManager = sessionManager;
        this.backgroundTaskManager = backgroundTaskManager;
        this.objectMapper = objectMapper;
        this.shellPolicy = agentRuntimeConfig.shellCommandPolicy();
    }

    @Tool(name = "Bash", converter = PlainTextToolResultConverter.class, description = TOOL_DESCRIPTION)
    public String bash(
            @ToolParam(name = "command", description = "The command to execute")
            String command,
            @ToolParam(name = "description", required = false, description = DESCRIPTION_PARAM)
            String description,
            @ToolParam(name = "timeout", required = false, description = "Optional timeout in milliseconds (max 600000)")
            Long timeout,
            @ToolParam(name = "run_in_background", required = false,
                    description = "Set to true to run this command in the background. Use BashOutput to read the output later.")
            Boolean runInBackground,
            RuntimeContext runtimeContext) {
        if (command == null || command.isBlank()) {
            return formatError("command is required");
        }
        String denied = shellPolicy.check(command);
        if (denied != null) {
            return formatError("Command rejected: " + denied);
        }
        if (Boolean.TRUE.equals(runInBackground)) {
            return startBackground(normalizeCommand(command.trim()), runtimeContext);
        }
        String normalizedCommand = normalizeCommand(command.trim());
        long timeoutMs = normalizeTimeout(normalizedCommand, timeout);
        String sessionId = sessionManager.resolveSessionId(runtimeContext);
        try {
            BashSession.BashExecutionResult result = sessionManager.execute(
                    sessionId, normalizedCommand, timeoutMs, MAX_OUTPUT_CHARS);
            return formatSuccess(result);
        } catch (BashSession.BashTimeoutException e) {
            return formatError(e.getMessage());
        } catch (Exception e) {
            return formatError(e.getMessage() == null ? "Bash command failed" : e.getMessage());
        }
    }

    @Tool(name = "BashOutput", converter = PlainTextToolResultConverter.class, description = BASH_OUTPUT_DESCRIPTION)
    public String bashOutput(
            @ToolParam(name = "bash_id", description = "The ID of the background shell to retrieve output from")
            String bashId,
            @ToolParam(name = "filter", required = false,
                    description = "Optional regular expression to filter the output lines. Only lines matching this regex will be included in the result. Any lines that do not match will no longer be available to read.")
            String filter) {
        try {
            BashBackgroundTask.BashOutputSnapshot snapshot =
                    backgroundTaskManager.readOutput(bashId, filter, MAX_OUTPUT_CHARS);
            return formatBackgroundOutput(snapshot);
        } catch (Exception e) {
            return formatError(e.getMessage() == null ? "BashOutput failed" : e.getMessage());
        }
    }

    @Tool(name = "KillShell", converter = PlainTextToolResultConverter.class, description = KILL_SHELL_DESCRIPTION)
    public String killShell(
            @ToolParam(name = "shell_id", description = "The ID of the background shell to kill")
            String shellId) {
        if (shellId == null || shellId.isBlank()) {
            return formatError("shell_id is required");
        }
        boolean killed = backgroundTaskManager.kill(shellId.trim());
        if (!killed) {
            return formatError("unknown or already finished shell_id: " + shellId.trim());
        }
        return "Shell " + shellId.trim() + " killed successfully.";
    }

    private String startBackground(String command, RuntimeContext runtimeContext) {
        try {
            String bashId = backgroundTaskManager.startBackground(
                    sessionManager.resolveSessionId(runtimeContext), command);
            return "Background shell started.\nbash_id: " + bashId;
        } catch (Exception e) {
            return formatError(e.getMessage() == null ? "failed to start background shell" : e.getMessage());
        }
    }

    private String formatSuccess(BashSession.BashExecutionResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("Exit code: ").append(result.exitCode()).append('\n');
        if (result.truncated()) {
            builder.append("(output truncated to ").append(MAX_OUTPUT_CHARS).append(" characters)\n");
        }
        builder.append('\n');
        if (result.output().isBlank()) {
            builder.append("(no output)");
        } else {
            builder.append(result.output());
        }
        return builder.toString();
    }

    private String formatBackgroundOutput(BashBackgroundTask.BashOutputSnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        builder.append("bash_id: ").append(snapshot.bashId()).append('\n');
        builder.append("status: ").append(snapshot.running() ? "running" : "completed").append('\n');
        if (!snapshot.running() && snapshot.exitCode() != null) {
            builder.append("exit code: ").append(snapshot.exitCode()).append('\n');
        }
        if (snapshot.truncated()) {
            builder.append("(output truncated to ").append(MAX_OUTPUT_CHARS).append(" characters)\n");
        }
        builder.append('\n');
        if (snapshot.output() == null || snapshot.output().isBlank()) {
            builder.append("(no new output)");
        } else {
            builder.append(snapshot.output());
        }
        return builder.toString();
    }

    private String formatError(String message) {
        return json(Map.of("ok", false, "message", message));
    }

    private String json(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            return "{\"ok\":false,\"message\":\"failed to serialize tool result\"}";
        }
    }

    private static String normalizeCommand(String command) {
        return BashSshSupport.normalizeRemoteSshCommand(command);
    }

    private static long normalizeTimeout(String command, Long timeout) {
        long timeoutMs = timeout == null || timeout <= 0 ? DEFAULT_TIMEOUT_MS : Math.min(timeout, MAX_TIMEOUT_MS);
        return BashSshSupport.normalizeTimeoutMs(command, timeoutMs);
    }
}
