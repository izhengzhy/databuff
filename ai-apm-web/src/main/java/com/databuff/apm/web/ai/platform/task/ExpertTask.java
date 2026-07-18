package com.databuff.apm.web.ai.platform.task;

import java.time.Instant;
import java.util.Map;

public record ExpertTask(
        String taskId,
        String parentTaskId,
        String sessionId,
        String sourceExpertId,
        String targetExpertId,
        ExpertTaskStatus status,
        String input,
        String output,
        String error,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {

    public ExpertTask {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public ExpertTask withStatus(
            ExpertTaskStatus nextStatus,
            String output,
            String error,
            Instant updatedAt,
            Instant completedAt) {
        return new ExpertTask(
                taskId, parentTaskId, sessionId, sourceExpertId, targetExpertId,
                nextStatus, input, output, error, metadata, createdAt, updatedAt, completedAt);
    }

    public ExpertTask withMetadata(Map<String, Object> nextMetadata) {
        return new ExpertTask(
                taskId, parentTaskId, sessionId, sourceExpertId, targetExpertId,
                status, input, output, error, nextMetadata, createdAt, updatedAt, completedAt);
    }
}
