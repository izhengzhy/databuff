package com.databuff.apm.web.tools.local;

import com.databuff.apm.web.ai.platform.runtime.ExpertChatContext;
import com.databuff.apm.web.ai.platform.runtime.ExpertChatScopeRegistry;
import com.databuff.apm.web.ai.platform.task.ExpertTaskContext;
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
        // Prefer exact runtime scope (sessionId#task:…) so parallel experts do not share a bash session.
        return ExpertTaskContext.sessionId()
                .filter(ExpertChatScopeRegistry::validSessionId)
                .or(() -> ExpertChatScopeRegistry.soleActiveState().map(ExpertChatContext.State::sessionId))
                .or(() -> ExpertChatScopeRegistry.soleSessionId())
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
