package com.databuff.apm.web.ai.platform.runtime;

import com.databuff.apm.web.ai.TestBeanSupport;
import com.databuff.apm.web.ai.agent.AgentRuntimeConfig;
import com.databuff.apm.web.ai.platform.skill.AiSkillDefinition;
import com.databuff.apm.web.ai.platform.skill.SkillManagementService;
import io.agentscope.core.skill.repository.FileSystemSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpertScopedSkillRepositoryTest {

    @TempDir
    Path tempDir;

    private FileSystemSkillRepository repository;

    @BeforeEach
    void setUp() {
        AgentRuntimeConfig runtimeConfig = new AgentRuntimeConfig();
        runtimeConfig.setCustomSkillsDir(tempDir.toString());
        SkillManagementService skillManagementService = TestBeanSupport.skillManagementService();
        SkillFileSyncService syncService = new SkillFileSyncService(
                runtimeConfig, skillManagementService, new DefaultResourceLoader());
        for (AiSkillDefinition skill : skillManagementService.list()) {
            syncService.syncSkill(skill);
        }
        repository = new FileSystemSkillRepository(runtimeConfig.builtinSkillsDirectory());
        assertThat(repository.getAllSkillNames())
                .contains("skill.brain.routing", "skill.data.metrics", "skill.inspection.health",
                        "skill.qa.product", "skill.summary.html");
    }

    @Test
    void exposesOnlyBoundSkillsForDataExpert() {
        ExpertScopedSkillRepository scoped = new ExpertScopedSkillRepository(
                repository, List.of("skill.data.metrics"));

        assertThat(scoped.getAllSkillNames()).containsExactly("skill.data.metrics");
        assertThat(scoped.getSkill("skill.data.metrics")).isNotNull();
        assertThat(scoped.skillExists("skill.data.metrics")).isTrue();
        assertThat(scoped.getSkill("skill.brain.routing")).isNull();
        assertThat(scoped.skillExists("skill.brain.routing")).isFalse();
        assertThat(scoped.getSkill("skill.inspection.health")).isNull();
        assertThat(scoped.skillExists("skill.inspection.health")).isFalse();
    }
}
