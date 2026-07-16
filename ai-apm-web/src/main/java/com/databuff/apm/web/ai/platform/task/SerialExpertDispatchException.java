package com.databuff.apm.web.ai.platform.task;

/**
 * Thrown when a second in-flight task would be created for the same session + target expert.
 * Callers should surface a busy wait message; do not treat this as accepting/deduping the new task.
 */
public final class SerialExpertDispatchException extends IllegalStateException {

    private final ExpertTask inFlightTask;

    public SerialExpertDispatchException(ExpertTask inFlightTask) {
        super("serial dispatch: targetExpertId=" + (inFlightTask == null ? "?" : inFlightTask.targetExpertId())
                + " already has in-flight taskId=" + (inFlightTask == null ? "?" : inFlightTask.taskId()));
        this.inFlightTask = inFlightTask;
    }

    public ExpertTask inFlightTask() {
        return inFlightTask;
    }
}
