package com.databuff.apm.web.ai.agent;

import com.databuff.apm.web.ai.platform.runtime.LayeredFilesystemSkillRepository;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Agent runtime registry. When {@code apm.agent.agentscope-enabled=true}, chat delegates to
 * {@link AgentScopeChatService}; otherwise {@link AgentBrainService} uses rule routing + HTTP LLM.
 */
@Component
@ConfigurationProperties(prefix = "apm.agent")
public class AgentRuntimeConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentRuntimeConfig.class);

    private boolean agentscopeEnabled;
    private String brainAgentName = "brain";
    private String dataAgentName = "data";
    private String inspectionAgentName = "inspection";
    /** Built-in skills from deploy/common/skills (read-only source tree). */
    private String builtinSkillsDir = "../deploy/common/skills";

    private static final List<String> BUILTIN_SKILLS_DIR_FALLBACKS = List.of(
            "../deploy/common/skills",
            "deploy/common/skills",
            "../../deploy/common/skills");
    /** Imported / synced custom skill packages (writable). */
    private String customSkillsDir = "./data/skills";
    private String workspaceDir = "./data/ai-workspaces";
    private String workspaceShellCommands = "cat,head,tail,grep,wc,ls,file,python3";
    private int workspaceShellTimeoutSeconds = 60;
    /** Max ReAct reasoning-tool loop iterations per expert turn. */
    private int maxIters = 1000;
    /** Per-request LLM HTTP timeout (OpenAI-compatible and AgentScope model calls). */
    private int llmHttpTimeoutSeconds = 300;
    /** Orchestrator wait for a full expert turn (stream or sync). */
    private int expertRoundTimeoutSeconds = 1200;

    @PostConstruct
    void logReady() {
        log.info(
                "Agent runtime ready (agentscopeEnabled={}, maxIters={}, llmHttpTimeoutSeconds={},"
                        + " expertRoundTimeoutSeconds={}, builtinSkillsDir={}, customSkillsDir={}, workspaceDir={})",
                agentscopeEnabled,
                maxIters,
                llmHttpTimeoutSeconds,
                expertRoundTimeoutSeconds,
                builtinSkillsDir,
                customSkillsDir,
                workspaceDir);
    }

    public boolean isAgentscopeEnabled() {
        return agentscopeEnabled;
    }

    public void setAgentscopeEnabled(boolean agentscopeEnabled) {
        this.agentscopeEnabled = agentscopeEnabled;
    }

    public String getBrainAgentName() {
        return brainAgentName;
    }

    public void setBrainAgentName(String brainAgentName) {
        this.brainAgentName = brainAgentName;
    }

    public String getDataAgentName() {
        return dataAgentName;
    }

    public void setDataAgentName(String dataAgentName) {
        this.dataAgentName = dataAgentName;
    }

    public String getInspectionAgentName() {
        return inspectionAgentName;
    }

    public void setInspectionAgentName(String inspectionAgentName) {
        this.inspectionAgentName = inspectionAgentName;
    }

    public String getBuiltinSkillsDir() {
        return builtinSkillsDir;
    }

    public void setBuiltinSkillsDir(String builtinSkillsDir) {
        this.builtinSkillsDir = builtinSkillsDir;
    }

    public String getCustomSkillsDir() {
        return customSkillsDir;
    }

    public void setCustomSkillsDir(String customSkillsDir) {
        this.customSkillsDir = customSkillsDir;
    }

    /** Writable directory for imported custom skill packages. */
    public Path customSkillsDirectory() {
        return Path.of(customSkillsDir).toAbsolutePath().normalize();
    }

    /** Built-in skill packages from deploy/common/skills. */
    public Path builtinSkillsDirectory() {
        return resolveExistingDirectory(builtinSkillsDir, BUILTIN_SKILLS_DIR_FALLBACKS)
                .orElseGet(() -> Path.of(builtinSkillsDir).toAbsolutePath().normalize());
    }

    private Optional<Path> resolveExistingDirectory(String configured, List<String> fallbacks) {
        Path primary = Path.of(configured).toAbsolutePath().normalize();
        if (Files.isDirectory(primary)) {
            return Optional.of(primary);
        }
        for (String fallback : fallbacks) {
            if (fallback.equals(configured)) {
                continue;
            }
            Path candidate = Path.of(fallback).toAbsolutePath().normalize();
            if (Files.isDirectory(candidate)) {
                log.warn("Configured builtin skills dir {} not found; using {}", primary, candidate);
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** Custom skill root first, then built-in deploy/common skills. */
    public List<Path> skillSearchDirectories() {
        List<Path> directories = new ArrayList<>();
        Path custom = customSkillsDirectory();
        if (Files.isDirectory(custom)) {
            directories.add(custom);
        }
        Path builtin = builtinSkillsDirectory();
        if (Files.isDirectory(builtin)) {
            directories.add(builtin);
        }
        return directories;
    }

    public AgentSkillRepository layeredSkillRepository() {
        return new LayeredFilesystemSkillRepository(skillSearchDirectories());
    }

    public String getWorkspaceDir() {
        return workspaceDir;
    }

    public void setWorkspaceDir(String workspaceDir) {
        this.workspaceDir = workspaceDir;
    }

    public String getWorkspaceShellCommands() {
        return workspaceShellCommands;
    }

    public void setWorkspaceShellCommands(String workspaceShellCommands) {
        this.workspaceShellCommands = workspaceShellCommands;
    }

    public int getWorkspaceShellTimeoutSeconds() {
        return workspaceShellTimeoutSeconds;
    }

    public void setWorkspaceShellTimeoutSeconds(int workspaceShellTimeoutSeconds) {
        this.workspaceShellTimeoutSeconds = workspaceShellTimeoutSeconds;
    }

    public int getMaxIters() {
        return maxIters;
    }

    public void setMaxIters(int maxIters) {
        this.maxIters = maxIters;
    }

    public int getLlmHttpTimeoutSeconds() {
        return llmHttpTimeoutSeconds;
    }

    public void setLlmHttpTimeoutSeconds(int llmHttpTimeoutSeconds) {
        this.llmHttpTimeoutSeconds = llmHttpTimeoutSeconds;
    }

    public int getExpertRoundTimeoutSeconds() {
        return expertRoundTimeoutSeconds;
    }

    public void setExpertRoundTimeoutSeconds(int expertRoundTimeoutSeconds) {
        this.expertRoundTimeoutSeconds = expertRoundTimeoutSeconds;
    }

    public Duration llmHttpTimeout() {
        return Duration.ofSeconds(Math.max(1, llmHttpTimeoutSeconds));
    }

    public Duration expertRoundTimeout() {
        return Duration.ofSeconds(Math.max(1, expertRoundTimeoutSeconds));
    }

    public long expertRoundTimeoutMillis() {
        return Math.max(1L, expertRoundTimeoutSeconds) * 1000L;
    }

    public int resolvedMaxIters() {
        return Math.max(1, maxIters);
    }

    public ExecutionConfig llmModelExecutionConfig() {
        return ExecutionConfig.mergeConfigs(
                ExecutionConfig.builder().timeout(llmHttpTimeout()).build(),
                ExecutionConfig.MODEL_DEFAULTS);
    }

    public Path workspaceDirectory() {
        return Path.of(workspaceDir).toAbsolutePath().normalize();
    }

    public Set<String> workspaceShellCommands() {
        if (workspaceShellCommands == null || workspaceShellCommands.isBlank()) {
            return Set.of("cat", "head", "tail", "grep", "wc", "ls", "file", "python3");
        }
        Set<String> commands = new LinkedHashSet<>();
        Arrays.stream(workspaceShellCommands.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .forEach(commands::add);
        return commands.isEmpty()
                ? Set.of("cat", "head", "tail", "grep", "wc", "ls", "file", "python3")
                : Set.copyOf(commands);
    }

    public RuntimeSummary summary() {
        String delegate = "AiChatOrchestrator";
        return new RuntimeSummary(
                agentscopeEnabled,
                brainAgentName,
                dataAgentName,
                inspectionAgentName,
                builtinSkillsDirectory().toString(),
                customSkillsDirectory().toString(),
                workspaceDirectory().toString(),
                delegate);
    }

    public record RuntimeSummary(
            boolean agentscopeEnabled,
            String brainAgent,
            String dataAgent,
            String inspectionAgent,
            String builtinSkillsDir,
            String customSkillsDir,
            String workspaceDir,
            String delegate) {
    }
}
