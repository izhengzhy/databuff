package com.databuff.apm.web.tools.local;

import com.databuff.apm.web.ai.platform.runtime.ExpertChatScopeRegistry;
import com.databuff.apm.web.ai.platform.task.ExpertSessionResolver;
import io.agentscope.core.agent.RuntimeContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class BashSessionManager {

    private final ConcurrentMap<String, BashSession> sessions = new ConcurrentHashMap<>();
    private final BashBackgroundTaskManager backgroundTaskManager;

    public BashSessionManager(BashBackgroundTaskManager backgroundTaskManager) {
        this.backgroundTaskManager = backgroundTaskManager;
    }

    public BashSession.BashExecutionResult execute(
            String sessionId,
            String command,
            long timeoutMs,
            int maxOutputChars) throws IOException, InterruptedException {
        return session(sessionId).execute(command, timeoutMs, maxOutputChars);
    }

    public void closeSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        String key = sessionId.trim();
        BashSession session = sessions.remove(key);
        if (session != null) {
            session.close();
        }
        backgroundTaskManager.closeSession(key);
    }

    public String resolveSessionId() {
        return resolveSessionId(null);
    }

    public String resolveSessionId(RuntimeContext runtimeContext) {
        // Priority: per-call RuntimeContext (most specific, threadsafe under concurrent chats)
        //          > "default" (last resort).
        // In production BashTools always passes RuntimeContext, so the RuntimeContext branch
        // resolves first. The previous ExpertTaskContext.sole* / ExpertChatScopeRegistry.sole*
        // fallbacks were removed because they are ambiguous when 2+ chats run concurrently on
        // the shared streamExecutor.
        return ExpertSessionResolver.sessionIdFromRuntimeContext(runtimeContext)
                .filter(ExpertChatScopeRegistry::validSessionId)
                .orElse("default");
    }

    private BashSession session(String sessionId) throws IOException {
        String key = sessionId == null || sessionId.isBlank() ? "default" : sessionId.trim();
        return sessions.computeIfAbsent(key, ignored -> {
            try {
                return new BashSession();
            } catch (IOException e) {
                throw new IllegalStateException("failed to start bash session", e);
            }
        });
    }
}
