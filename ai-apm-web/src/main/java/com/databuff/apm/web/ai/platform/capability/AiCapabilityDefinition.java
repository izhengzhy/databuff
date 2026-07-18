package com.databuff.apm.web.ai.platform.capability;

import java.time.Instant;
import java.util.List;

/**
 * 落地页 7 个 AI 能力弧线的运行时定义。
 * 对应文章《开源 AIOps，终于有人做出来了》的 7 个体验场景。
 * 默认值（default*）来自数据库种子，"恢复默认" 把 default* 回写到当前字段。
 */
public record AiCapabilityDefinition(
        String capabilityId,
        String name,
        String tagline,
        String color,
        String expertId,
        List<String> prompts,
        boolean enabled,
        boolean builtIn,
        long version,
        Instant createdAt,
        Instant updatedAt,
        String defaultName,
        String defaultTagline,
        String defaultExpertId,
        List<String> defaultPrompts) {

    public AiCapabilityDefinition {
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
        defaultPrompts = defaultPrompts == null ? List.of() : List.copyOf(defaultPrompts);
    }

    public AiCapabilityDefinition withVersion(long nextVersion, Instant updatedAt) {
        return new AiCapabilityDefinition(
                capabilityId, name, tagline, color, expertId, prompts,
                enabled, builtIn, nextVersion, createdAt, updatedAt,
                defaultName, defaultTagline, defaultExpertId, defaultPrompts);
    }
}
