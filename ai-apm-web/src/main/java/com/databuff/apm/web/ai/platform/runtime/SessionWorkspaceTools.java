package com.databuff.apm.web.ai.platform.runtime;

import com.databuff.apm.web.ai.agent.AgentRuntimeConfig;
import com.databuff.apm.web.ai.agent.ShellCommandPolicy;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Component
@Lazy
public class SessionWorkspaceTools {

    private final SessionWorkspaceService workspaceService;
    private final Set<String> allowedShellCommands;
    private final int shellTimeoutSeconds;
    private final ShellCommandPolicy shellPolicy;

    public SessionWorkspaceTools(
            SessionWorkspaceService workspaceService,
            AgentRuntimeConfig agentRuntimeConfig) {
        this.workspaceService = workspaceService;
        this.allowedShellCommands = agentRuntimeConfig.workspaceShellCommands();
        this.shellTimeoutSeconds = agentRuntimeConfig.getWorkspaceShellTimeoutSeconds();
        this.shellPolicy = agentRuntimeConfig.shellCommandPolicy();
    }

    @Tool(description = "List files in the current chat session workspace (uploads, outputs) or skill resources (relativePath=resources/{skillId}/...)")
    public String listWorkspaceFiles(
            @ToolParam(name = "relativePath", description = "Relative path under session workspace, e.g. uploads, outputs, or resources/skill.summary.html/templates")
            String relativePath) {
        String sessionId = requireSessionId();
        try {
            String listingPath = normalizeListingPath(relativePath);
            Path dir = workspaceService.resolveRelativePath(sessionId, listingPath);
            if (!Files.isDirectory(dir)) {
                return "not a directory: " + listingPath;
            }
            try (Stream<Path> stream = Files.list(dir)) {
                List<Path> entries = stream.sorted(Comparator.comparing(Path::getFileName)).toList();
                if (entries.isEmpty()) {
                    return "(empty)";
                }
                StringBuilder builder = new StringBuilder();
                boolean resources = workspaceService.isResourcesPath(listingPath);
                Path sessionRoot = resources ? null : workspaceService.sessionDir(sessionId);
                String resourcesPrefix = resources
                        ? (listingPath.endsWith("/") ? listingPath.substring(0, listingPath.length() - 1) : listingPath)
                        : null;
                for (Path entry : entries) {
                    builder.append(Files.isDirectory(entry) ? "[dir] " : "[file] ");
                    if (resources) {
                        builder.append(resourcesPrefix).append('/').append(entry.getFileName());
                    } else {
                        builder.append(sessionRoot.relativize(entry).toString().replace('\\', '/'));
                    }
                    builder.append('\n');
                }
                return builder.toString().trim();
            }
        } catch (Exception e) {
            return "listWorkspaceFiles failed: " + e.getMessage();
        }
    }

    @Tool(description = "Read a text or image file from the current chat session workspace or skill resources (e.g. resources/skill.summary.html/templates/inspection-report.html)")
    public ToolResultBlock readWorkspaceFile(
            @ToolParam(name = "filePath", description = "Relative file path, e.g. uploads/report.csv or resources/skill.summary.html/templates/summary-brief.html")
            String filePath,
            @ToolParam(name = "lineRange", description = "Optional line range for text files, e.g. 1-200")
            String lineRange) {
        String sessionId = requireSessionId();
        try {
            Path file = workspaceService.resolveRelativePath(sessionId, filePath);
            if (!Files.isRegularFile(file)) {
                return ToolResultBlock.text("not a file: " + filePath);
            }
            if (WorkspaceImageSupport.isImageFile(file)) {
                return readWorkspaceImage(sessionId, filePath);
            }
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int[] range = parseLineRange(lineRange, lines.size());
            StringBuilder builder = new StringBuilder();
            for (int i = range[0]; i <= range[1]; i++) {
                builder.append(i + 1).append(": ").append(lines.get(i)).append('\n');
            }
            return ToolResultBlock.text(builder.toString().trim());
        } catch (Exception e) {
            return ToolResultBlock.text("readWorkspaceFile failed: " + e.getMessage());
        }
    }

    private ToolResultBlock readWorkspaceImage(String sessionId, String filePath) {
        return WorkspaceImageSupport.readImageBlock(workspaceService, sessionId, filePath)
                .map(imageBlock -> ToolResultBlock.of(List.of(
                        TextBlock.builder().text("Image file: " + filePath).build(),
                        imageBlock)))
                .orElse(ToolResultBlock.text("readWorkspaceFile failed: unsupported or unreadable image: " + filePath));
    }

    @Tool(description = "Write a text file to outputs/ in the current session workspace for user download/preview (any text suffix: .html, .md, .csv, .json, ...)")
    public String writeWorkspaceFile(
            @ToolParam(name = "fileName", description = "File name or outputs/ path, e.g. summary.html or report.csv")
            String fileName,
            @ToolParam(name = "content", description = "Text content to write")
            String content,
            @ToolParam(name = "mode", description = "overwrite or append, default overwrite")
            String mode) {
        String sessionId = requireSessionId();
        if (content == null) {
            return "content is required";
        }
        try {
            workspaceService.ensureOutputsDir(sessionId);
            String relativePath = workspaceService.resolveOutputWritePath(fileName);
            Path target = workspaceService.resolveRelativePath(sessionId, relativePath);
            Files.createDirectories(target.getParent());
            String writeMode = mode == null || mode.isBlank() ? "overwrite" : mode.trim();
            if ("append".equalsIgnoreCase(writeMode)) {
                Files.writeString(target, content, StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } else {
                Files.writeString(target, content, StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            }
            return "Wrote " + relativePath;
        } catch (Exception e) {
            return "writeWorkspaceFile failed: " + e.getMessage();
        }
    }

    @Tool(description = "Execute an allowed shell command in the current chat session workspace")
    public String executeWorkspaceShell(
            @ToolParam(name = "command", description = "Shell command, e.g. head -n 20 uploads/log.txt")
            String command,
            @ToolParam(name = "timeoutSeconds", description = "Optional timeout in seconds")
            Integer timeoutSeconds) {
        String sessionId = requireSessionId();
        if (command == null || command.isBlank()) {
            return "command is required";
        }
        String normalized = command.trim();
        String rootCommand = normalized.split("\\s+", 2)[0];
        if (!allowedShellCommands.contains(rootCommand)) {
            return "command is not allowed: " + rootCommand;
        }
        String denied = shellPolicy.check(normalized);
        if (denied != null) {
            return "command is not allowed: " + denied;
        }
        int timeout = timeoutSeconds == null || timeoutSeconds <= 0
                ? shellTimeoutSeconds
                : Math.min(timeoutSeconds, shellTimeoutSeconds);
        try {
            Process process = new ProcessBuilder("/bin/sh", "-lc", normalized)
                    .directory(workspaceService.sessionDir(sessionId).toFile())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "command timed out after " + timeout + "s";
            }
            String output = readProcessOutput(process);
            if (output.isBlank()) {
                return "exit " + process.exitValue();
            }
            return output;
        } catch (Exception e) {
            return "executeWorkspaceShell failed: " + e.getMessage();
        }
    }

    private static String requireSessionId() {
        // Workspace is keyed by logical parent session; strip #task: if only a subtask scope is active.
        return ExpertChatScopeRegistry.soleSessionId()
                .or(() -> ExpertChatScopeRegistry.soleActiveState()
                        .map(state -> ExpertChatScopeRegistry.parentSessionId(state.sessionId())))
                .filter(id -> id != null && !id.isBlank())
                .orElseThrow(() -> new IllegalStateException("session workspace is unavailable outside chat context"));
    }

    private static String normalizeListingPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return ".";
        }
        return relativePath.trim();
    }

    private static int[] parseLineRange(String lineRange, int totalLines) {
        if (totalLines <= 0) {
            return new int[]{0, -1};
        }
        if (lineRange == null || lineRange.isBlank()) {
            return new int[]{0, totalLines - 1};
        }
        String[] parts = lineRange.trim().split("-", 2);
        int start = parsePositiveInt(parts[0], 1);
        int end = parts.length > 1 ? parsePositiveInt(parts[1], totalLines) : start;
        start = Math.max(1, Math.min(start, totalLines));
        end = Math.max(start, Math.min(end, totalLines));
        return new int[]{start - 1, end - 1};
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String readProcessOutput(Process process) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return String.join("\n", lines).trim();
    }
}
