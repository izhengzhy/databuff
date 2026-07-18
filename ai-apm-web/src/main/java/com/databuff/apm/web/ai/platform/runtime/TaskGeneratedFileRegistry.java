package com.databuff.apm.web.ai.platform.runtime;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Records output files at the point where a task writes them.
 *
 * <p>The task service consumes these paths when it commits the task deliverable,
 * which gives each generated file an explicit owner instead of inferring ownership
 * later by scanning the shared session workspace.</p>
 */
@Component
public class TaskGeneratedFileRegistry {

    private final ConcurrentMap<String, Set<String>> pathsByTask = new ConcurrentHashMap<>();

    public void record(String sessionId, String taskId, String relativePath) {
        if (blank(sessionId) || blank(taskId) || blank(relativePath)) {
            return;
        }
        pathsByTask.computeIfAbsent(key(sessionId, taskId), ignored -> ConcurrentHashMap.newKeySet())
                .add(relativePath.trim());
    }

    public List<String> paths(String sessionId, String taskId) {
        if (blank(sessionId) || blank(taskId)) {
            return List.of();
        }
        Set<String> paths = pathsByTask.get(key(sessionId, taskId));
        return paths == null ? List.of() : paths.stream().sorted().toList();
    }

    public void remove(String sessionId, String taskId) {
        if (!blank(sessionId) && !blank(taskId)) {
            pathsByTask.remove(key(sessionId, taskId));
        }
    }

    public void removeSession(String sessionId) {
        if (blank(sessionId)) {
            return;
        }
        String prefix = sessionId.trim() + ":";
        pathsByTask.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private static String key(String sessionId, String taskId) {
        return sessionId.trim() + ":" + taskId.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
