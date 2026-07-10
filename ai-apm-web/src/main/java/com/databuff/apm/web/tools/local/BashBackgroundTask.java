package com.databuff.apm.web.tools.local;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class BashBackgroundTask {

    private final String id;
    private final String sessionId;
    private final String command;
    private final Process process;
    private final StringBuilder output = new StringBuilder();
    private final Object outputLock = new Object();
    private volatile int deliveredLength;
    private volatile boolean running = true;
    private volatile Integer exitCode;
    private final Thread readerThread;

    BashBackgroundTask(String id, String sessionId, String command) throws IOException {
        this.id = id;
        this.sessionId = sessionId;
        this.command = command;
        ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-lc", command);
        builder.redirectErrorStream(true);
        this.process = builder.start();
        this.readerThread = new Thread(this::drainOutput, "bash-bg-" + id);
        this.readerThread.setDaemon(true);
        this.readerThread.start();
        Thread watcher = new Thread(this::waitForExit, "bash-bg-wait-" + id);
        watcher.setDaemon(true);
        watcher.start();
    }

    String id() {
        return id;
    }

    String sessionId() {
        return sessionId;
    }

    boolean running() {
        return running;
    }

    Integer exitCode() {
        return exitCode;
    }

    BashOutputSnapshot readOutput(String filterRegex, int maxChars) {
        synchronized (outputLock) {
            String pending = output.substring(deliveredLength);
            if (pending.isEmpty()) {
                return snapshot("", false);
            }
            if (running) {
                int lastNewline = pending.lastIndexOf('\n');
                if (lastNewline < 0) {
                    return snapshot("", false);
                }
                pending = pending.substring(0, lastNewline + 1);
            }
            Pattern pattern = compileFilter(filterRegex);
            String[] lines = pending.split("\n", -1);
            StringBuilder selected = new StringBuilder();
            for (int i = 0; i < lines.length; i++) {
                if (i == lines.length - 1 && lines[i].isEmpty()) {
                    break;
                }
                String line = lines[i];
                if (pattern == null || pattern.matcher(line).find()) {
                    if (!selected.isEmpty()) {
                        selected.append('\n');
                    }
                    selected.append(line);
                }
            }
            deliveredLength += pending.length();
            String chunk = selected.toString();
            boolean truncated = false;
            if (chunk.length() > maxChars) {
                chunk = chunk.substring(0, maxChars);
                truncated = true;
            }
            return snapshot(chunk, truncated);
        }
    }

    boolean kill() {
        if (!process.isAlive()) {
            running = false;
            return false;
        }
        process.destroyForcibly();
        running = false;
        if (exitCode == null) {
            exitCode = -1;
        }
        return true;
    }

    void close() {
        kill();
        try {
            readerThread.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void drainOutput() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                synchronized (outputLock) {
                    output.append(buffer, 0, read);
                }
            }
        } catch (IOException ignored) {
            // Process ended or was killed.
        }
    }

    private void waitForExit() {
        try {
            boolean finished = process.waitFor(1, TimeUnit.HOURS);
            if (!finished) {
                process.destroyForcibly();
            }
            exitCode = process.exitValue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            exitCode = -1;
        } finally {
            running = false;
        }
    }

    private BashOutputSnapshot snapshot(String text, boolean truncated) {
        return new BashOutputSnapshot(id, running, exitCode, text, truncated);
    }

    private static Pattern compileFilter(String filterRegex) {
        if (filterRegex == null || filterRegex.isBlank()) {
            return null;
        }
        try {
            return Pattern.compile(filterRegex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("invalid filter regex: " + e.getMessage());
        }
    }

    record BashOutputSnapshot(
            String bashId,
            boolean running,
            Integer exitCode,
            String output,
            boolean truncated) {
    }
}
