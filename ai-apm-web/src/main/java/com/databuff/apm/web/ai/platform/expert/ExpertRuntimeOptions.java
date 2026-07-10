package com.databuff.apm.web.ai.platform.expert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpertRuntimeOptions(
        String category,
        boolean stream,
        boolean enablePlan,
        boolean dynamicSkillsEnabled,
        boolean exposeToolEvents,
        ExpertToolAccessMode toolAccessMode) {

    public ExpertRuntimeOptions {
        if (category == null || category.isBlank()) {
            category = "默认分类";
        }
        if (toolAccessMode == null) {
            toolAccessMode = ExpertToolAccessMode.ALLOWLIST;
        }
    }

    public static ExpertRuntimeOptions defaults() {
        return new ExpertRuntimeOptions("默认分类", false, false, false, true, ExpertToolAccessMode.ALLOWLIST);
    }
}
