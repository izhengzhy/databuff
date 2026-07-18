package com.databuff.apm.web.ai.platform.task;

import com.databuff.apm.web.ai.platform.runtime.ExpertChatScopeRegistry;
import io.agentscope.core.agent.RuntimeContext;

import java.util.Optional;

public final class ExpertSessionResolver {

    private ExpertSessionResolver() {
    }

    public static Optional<String> resolveSessionId(String hintSessionId, RuntimeContext runtimeContext) {
        return optionalChatSessionId(hintSessionId)
                .or(() -> sessionIdFromRuntimeContext(runtimeContext));
    }

    public static Optional<String> sessionIdFromRuntimeContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            return Optional.empty();
        }
        return optionalChatSessionId(runtimeContext.getSessionId())
                .or(() -> {
                    Object extra = runtimeContext.get(ExpertMessageConstants.META_SESSION_ID);
                    if (extra == null) {
                        extra = runtimeContext.get("sessionId");
                    }
                    return optionalChatSessionId(extra == null ? null : String.valueOf(extra));
                });
    }

    public static String normalizeChatSessionId(String sessionId) {
        return ExpertChatScopeRegistry.parentSessionId(sessionId);
    }

    private static Optional<String> optionalChatSessionId(String sessionId) {
        String normalized = normalizeChatSessionId(sessionId);
        return normalized == null ? Optional.empty() : Optional.of(normalized);
    }

    public static String resolveSessionIdOrThrow(String hintSessionId, RuntimeContext runtimeContext) {
        return resolveSessionId(hintSessionId, runtimeContext)
                .orElseThrow(() -> new IllegalStateException(
                        "sessionId unavailable for expert dispatch; ensure brain runtime context is bound"));
    }
}
