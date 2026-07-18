package com.databuff.apm.web.ai.platform.capability;

import com.databuff.apm.web.ai.platform.expert.ExpertManagementService;
import com.databuff.apm.web.persistence.AiPlatformPersistence;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CapabilityManagementService {

    private static final Logger log = LoggerFactory.getLogger(CapabilityManagementService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    @Autowired
    private ObjectProvider<AiPlatformPersistence> persistence;
    @Autowired
    private ObjectProvider<ExpertManagementService> expertManagementService;

    private final ConcurrentMap<String, AiCapabilityDefinition> capabilities = new ConcurrentHashMap<>();

    public List<AiCapabilityDefinition> list() {
        return capabilities.values().stream()
                .sorted(Comparator.comparing(AiCapabilityDefinition::capabilityId))
                .toList();
    }

    public List<AiCapabilityDefinition> listEnabled() {
        return list().stream().filter(AiCapabilityDefinition::enabled).toList();
    }

    public Optional<AiCapabilityDefinition> find(String capabilityId) {
        return Optional.ofNullable(capabilities.get(capabilityId));
    }

    public AiCapabilityDefinition save(AiCapabilityDefinition definition) {
        validate(definition);
        Instant now = Instant.now();
        AiCapabilityDefinition saved = capabilities.compute(definition.capabilityId(), (id, existing) -> {
            if (existing == null) {
                long version = definition.version() <= 0 ? 1L : definition.version();
                // 新建能力：默认值 = 当前值
                return new AiCapabilityDefinition(
                        definition.capabilityId(), definition.name(),
                        definition.tagline(), definition.color(), definition.expertId(), definition.prompts(),
                        definition.enabled(), definition.builtIn(), version, now, now,
                        definition.name(), definition.tagline(), definition.expertId(), definition.prompts());
            }
            String color = existing.color();
            boolean builtIn = existing.builtIn();
            // 编辑已有能力：保留原默认值
            return new AiCapabilityDefinition(
                    existing.capabilityId(), definition.name(), definition.tagline(), color,
                    definition.expertId(), definition.prompts(), definition.enabled(), builtIn,
                    existing.version() + 1, existing.createdAt(), now,
                    existing.defaultName(), existing.defaultTagline(), existing.defaultExpertId(),
                    existing.defaultPrompts());
        });
        ifAvailable(sync -> sync.persistCapability(saved));
        return saved;
    }

    public AiCapabilityDefinition setEnabled(String capabilityId, boolean enabled) {
        AiCapabilityDefinition existing = capabilities.get(capabilityId);
        if (existing == null) {
            throw new IllegalArgumentException("capability not found: " + capabilityId);
        }
        return save(new AiCapabilityDefinition(
                existing.capabilityId(), existing.name(), existing.tagline(),
                existing.color(), existing.expertId(), existing.prompts(),
                enabled, existing.builtIn(), existing.version(), existing.createdAt(), existing.updatedAt(),
                existing.defaultName(), existing.defaultTagline(), existing.defaultExpertId(),
                existing.defaultPrompts()));
    }

    public AiCapabilityDefinition resetToDefault(String capabilityId) {
        AiCapabilityDefinition existing = capabilities.get(capabilityId);
        if (existing == null) {
            throw new IllegalArgumentException("capability not found: " + capabilityId);
        }
        Instant now = Instant.now();
        AiCapabilityDefinition reset = new AiCapabilityDefinition(
                existing.capabilityId(),
                existing.defaultName(), existing.defaultTagline(), existing.color(),
                existing.defaultExpertId(), existing.defaultPrompts(),
                true, existing.builtIn(), existing.version() + 1, existing.createdAt(), now,
                existing.defaultName(), existing.defaultTagline(), existing.defaultExpertId(),
                existing.defaultPrompts());
        capabilities.put(capabilityId, reset);
        ifAvailable(sync -> sync.persistCapability(reset));
        return reset;
    }

    public void applyPersistedRows(List<AiCapabilityDefinition> definitions) {
        for (AiCapabilityDefinition definition : definitions) {
            capabilities.compute(definition.capabilityId(), (id, existing) -> {
                boolean builtIn = (existing != null && existing.builtIn()) || definition.builtIn();
                String color = existing != null ? existing.color() : definition.color();
                return new AiCapabilityDefinition(
                        definition.capabilityId(), definition.name(), definition.tagline(), color,
                        definition.expertId(), definition.prompts(), definition.enabled(), builtIn,
                        definition.version(), definition.createdAt(), definition.updatedAt(),
                        definition.defaultName(), definition.defaultTagline(), definition.defaultExpertId(),
                        definition.defaultPrompts());
            });
        }
        log.info("Applied {} persisted capability rows", definitions.size());
    }

    private void validate(AiCapabilityDefinition definition) {
        if (definition == null || blank(definition.capabilityId()) || blank(definition.name())) {
            throw new IllegalArgumentException("capabilityId and name are required");
        }
        // expertId 可选：不绑定专家时留空，对话页点击该步骤不切换专家
        if (!blank(definition.expertId()) && expertManagementService != null) {
            try {
                ExpertManagementService experts = expertManagementService.getObject();
                if (experts != null && experts.find(definition.expertId()).isEmpty()) {
                    throw new IllegalArgumentException("expert not found: " + definition.expertId());
                }
            } catch (Exception ignored) {
                // ExpertManagementService not available (e.g. Doris down) — skip cross-validation
            }
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void ifAvailable(java.util.function.Consumer<AiPlatformPersistence> consumer) {
        if (persistence != null) {
            persistence.ifAvailable(consumer);
        }
    }

    public static List<String> readPrompts(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, STRING_LIST);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid prompts json", e);
        }
    }

    public static String writePrompts(List<String> prompts) {
        try {
            return OBJECT_MAPPER.writeValueAsString(prompts == null ? List.of() : prompts);
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to write prompts json", e);
        }
    }
}
