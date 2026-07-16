package com.databuff.apm.web.ai.platform.runtime;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.state.AgentState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recovers AgentScope sessions that were left waiting for MCP tool confirmation.
 */
final class AgentScopeToolConfirmSupport {

    private AgentScopeToolConfirmSupport() {
    }

    static Msg attachAutoConfirmIfNeeded(ReActAgent agent, Msg userMessage) {
        if (agent == null || userMessage == null) {
            return userMessage;
        }
        List<ConfirmResult> pending = pendingConfirmResults(agent);
        if (pending.isEmpty()) {
            return userMessage;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (userMessage.getMetadata() != null) {
            metadata.putAll(userMessage.getMetadata());
        }
        metadata.put(Msg.METADATA_CONFIRM_RESULTS, pending);
        // Keep existing content blocks (including ImageBlock). Do not call textContent(),
        // which replaces the whole content list with a single TextBlock.
        return Msg.builder()
                .id(userMessage.getId())
                .name(userMessage.getName())
                .role(userMessage.getRole())
                .content(userMessage.getContent())
                .metadata(metadata)
                .timestamp(userMessage.getTimestamp())
                .usage(userMessage.getUsage())
                .build();
    }

    static List<ConfirmResult> pendingConfirmResults(ReActAgent agent) {
        AgentState state = agent.getAgentState();
        if (state == null || state.getContext() == null || state.getContext().isEmpty()) {
            return List.of();
        }
        Msg lastAssistant = findLastAssistant(state.getContext());
        if (lastAssistant == null) {
            return List.of();
        }
        List<ConfirmResult> results = new ArrayList<>();
        for (ToolUseBlock toolCall : lastAssistant.getContentBlocks(ToolUseBlock.class)) {
            if (toolCall != null && toolCall.getState() == ToolCallState.ASKING) {
                results.add(new ConfirmResult(true, toolCall));
            }
        }
        return List.copyOf(results);
    }

    private static Msg findLastAssistant(List<Msg> context) {
        for (int index = context.size() - 1; index >= 0; index--) {
            Msg message = context.get(index);
            if (message != null && message.getRole() == MsgRole.ASSISTANT) {
                return message;
            }
        }
        return null;
    }
}
