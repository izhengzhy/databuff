package com.databuff.apm.web.tools.local;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class BashBackgroundTaskManager {

    private final ConcurrentMap<String, BashBackgroundTask> tasksById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> taskIdsBySession = new ConcurrentHashMap<>();

    public String startBackground(String sessionId, String command) throws IOException {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command is required");
        }
        String key = normalizeSessionId(sessionId);
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        BashBackgroundTask task = new BashBackgroundTask(taskId, key, command.trim());
        tasksById.put(taskId, task);
        taskIdsBySession.computeIfAbsent(key, ignored -> new CopyOnWriteArraySet<>()).add(taskId);
        return taskId;
    }

    public BashBackgroundTask.BashOutputSnapshot readOutput(String bashId, String filter, int maxChars) {
        BashBackgroundTask task = requireTask(bashId);
        return task.readOutput(filter, maxChars);
    }

    public boolean kill(String shellId) {
        BashBackgroundTask task = tasksById.get(normalizeId(shellId));
        if (task == null) {
            return false;
        }
        boolean killed = task.kill();
        tasksById.remove(task.id());
        Set<String> sessionTasks = taskIdsBySession.get(task.sessionId());
        if (sessionTasks != null) {
            sessionTasks.remove(task.id());
        }
        return killed;
    }

    public void closeSession(String sessionId) {
        String key = normalizeSessionId(sessionId);
        Set<String> ids = taskIdsBySession.remove(key);
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            BashBackgroundTask task = tasksById.remove(id);
            if (task != null) {
                task.close();
            }
        }
    }

    private BashBackgroundTask requireTask(String bashId) {
        BashBackgroundTask task = tasksById.get(normalizeId(bashId));
        if (task == null) {
            throw new IllegalArgumentException("unknown bash_id: " + bashId);
        }
        return task;
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("bash_id is required");
        }
        return id.trim();
    }

    private static String normalizeSessionId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "default" : sessionId.trim();
    }
}
