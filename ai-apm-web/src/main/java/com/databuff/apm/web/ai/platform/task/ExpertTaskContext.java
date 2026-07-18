package com.databuff.apm.web.ai.platform.task;

import com.databuff.apm.web.ai.platform.runtime.ExpertChatScopeRegistry;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Dispatcher stack for expert tasks.
 * <p>
 * Parent brain chat pushes under the logical {@code sessionId}. Each subtask pushes under
 * {@code sessionId#task:{taskId}} so parallel experts never share / corrupt one stack.
 */
public final class ExpertTaskContext {

    private static final ConcurrentMap<String, ConcurrentLinkedDeque<State>> SCOPES = new ConcurrentHashMap<>();

    private ExpertTaskContext() {
    }

    public static <T> T run(
            String sessionId,
            String sourceExpertId,
            Consumer<ExpertTaskEvent> eventSink,
            java.util.function.Supplier<T> action) {
        ConcurrentLinkedDeque<State> stack = SCOPES.computeIfAbsent(sessionId, key -> new ConcurrentLinkedDeque<>());
        stack.push(new State(sessionId, sourceExpertId, eventSink));
        try {
            return action.get();
        } finally {
            stack.poll();
            if (stack.isEmpty()) {
                SCOPES.remove(sessionId, stack);
            }
        }
    }

    public static void runVoid(
            String sessionId,
            String sourceExpertId,
            Consumer<ExpertTaskEvent> eventSink,
            Runnable action) {
        run(sessionId, sourceExpertId, eventSink, () -> {
            action.run();
            return null;
        });
    }

    public static void enterScope(
            String sessionId,
            String sourceExpertId,
            Consumer<ExpertTaskEvent> eventSink) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        String key = sessionId.trim();
        ConcurrentLinkedDeque<State> stack = SCOPES.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());
        stack.push(new State(key, sourceExpertId, eventSink));
    }

    public static void leaveScope(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        String key = sessionId.trim();
        ConcurrentLinkedDeque<State> stack = SCOPES.get(key);
        if (stack == null) {
            return;
        }
        stack.poll();
        if (stack.isEmpty()) {
            SCOPES.remove(key, stack);
        }
    }

    public static Optional<String> sessionId(String sessionId) {
        return peekStack(sessionId).map(State::sessionId);
    }

    public static Optional<String> sourceExpertId(String sessionId) {
        return peekStack(sessionId).map(State::sourceExpertId);
    }

    /**
     * Outermost (bottom-of-stack) source expert for a logical session — the original dispatcher
     * (typically brain). Looks at the parent stack only; task-scoped frames are ignored here.
     */
    public static Optional<String> outermostSourceExpertId(String sessionId) {
        String parent = ExpertChatScopeRegistry.parentSessionId(sessionId);
        if (parent == null) {
            return Optional.empty();
        }
        ConcurrentLinkedDeque<State> stack = SCOPES.get(parent);
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        State bottom = stack.peekLast();
        if (bottom == null || blank(bottom.sourceExpertId())) {
            return Optional.empty();
        }
        return Optional.of(bottom.sourceExpertId());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public static void emit(String sessionId, ExpertTaskEvent event) {
        if (sessionId == null || sessionId.isBlank() || event == null) {
            return;
        }
        String parent = ExpertChatScopeRegistry.parentSessionId(sessionId);
        if (parent == null) {
            return;
        }
        // Parent brain chat sink lives on the logical session stack (top = active SSE).
        ConcurrentLinkedDeque<State> stack = SCOPES.get(parent);
        if (stack == null) {
            return;
        }
        State state = stack.peek();
        if (state != null && state.eventSink != null) {
            state.eventSink.accept(event);
        }
    }

    private static Optional<State> peekStack(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        ConcurrentLinkedDeque<State> stack = SCOPES.get(sessionId.trim());
        if (stack == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.peek());
    }

    /** Clears all task scopes; for unit tests only. */
    public static void clearForTests() {
        SCOPES.clear();
    }

    private record State(
            String sessionId,
            String sourceExpertId,
            Consumer<ExpertTaskEvent> eventSink) {
    }
}
