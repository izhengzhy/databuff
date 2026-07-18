package com.databuff.apm.web.ai.platform.api;

import com.databuff.apm.web.ai.platform.AiPlatformApiException;
import com.databuff.apm.web.ai.platform.capability.AiCapabilityDefinition;
import com.databuff.apm.web.ai.platform.capability.CapabilityManagementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai/capabilities")
public class AiCapabilityController {

    private final CapabilityManagementService capabilityManagementService;

    public AiCapabilityController(CapabilityManagementService capabilityManagementService) {
        this.capabilityManagementService = capabilityManagementService;
    }

    @GetMapping
    public List<AiCapabilityDefinition> list() {
        return capabilityManagementService.list();
    }

    @GetMapping("/{capabilityId}")
    public AiCapabilityDefinition get(@PathVariable String capabilityId) {
        return capabilityManagementService.find(capabilityId)
                .orElseThrow(() -> AiPlatformApiException.notFound("capability", capabilityId));
    }

    @PutMapping("/{capabilityId}")
    public AiCapabilityDefinition update(@PathVariable String capabilityId, @RequestBody SaveCapabilityRequest request) {
        AiCapabilityDefinition existing = capabilityManagementService.find(capabilityId)
                .orElseThrow(() -> AiPlatformApiException.notFound("capability", capabilityId));
        SaveCapabilityRequest merged = request == null ? new SaveCapabilityRequest(
                existing.name(), existing.tagline(), existing.expertId(), existing.prompts(), existing.enabled())
                : request;
        AiCapabilityDefinition definition = new AiCapabilityDefinition(
                capabilityId, merged.name(), merged.tagline(), existing.color(),
                merged.expertId == null ? "" : merged.expertId,
                merged.prompts == null ? List.of() : merged.prompts,
                merged.enabled == null || merged.enabled, existing.builtIn(), existing.version(),
                existing.createdAt(), Instant.now(),
                existing.defaultName(), existing.defaultTagline(), existing.defaultExpertId(),
                existing.defaultPrompts());
        return capabilityManagementService.save(definition);
    }

    @PostMapping("/{capabilityId}/enable")
    public AiCapabilityDefinition enable(@PathVariable String capabilityId) {
        return capabilityManagementService.setEnabled(capabilityId, true);
    }

    @PostMapping("/{capabilityId}/disable")
    public AiCapabilityDefinition disable(@PathVariable String capabilityId) {
        return capabilityManagementService.setEnabled(capabilityId, false);
    }

    @PostMapping("/{capabilityId}/reset")
    public AiCapabilityDefinition reset(@PathVariable String capabilityId) {
        return capabilityManagementService.resetToDefault(capabilityId);
    }

    public record SaveCapabilityRequest(
            String name,
            String tagline,
            String expertId,
            List<String> prompts,
            Boolean enabled) {
    }
}
