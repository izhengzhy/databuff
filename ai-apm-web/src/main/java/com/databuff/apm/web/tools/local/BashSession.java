package com.databuff.apm.web.tools.local;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Persistent bash process for one chat session (Anthropic bash-tool sentinel pattern).
 */
final class BashSession implements AutoCloseable {

    private final Object lock = new Object();
    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;
    private volatile boolean closed;

    BashSession() throws IOException {
        start();
    }

    BashExecutionResult execute(String command, long timeoutMs, int maxOutputChars) throws IOException, InterruptedException {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command is required");
        }
        synchronized (lock) {
            ensureAlive();
            long effectiveTimeout = clampTimeout(timeoutMs);
            String marker = "__DATABUFF_BASH_" + UUID.randomUUID().toString().replace("-", "") + "__";
            // Redirect stdin so ssh/sshpass children cannot block on the session command pipe (deadlock).
            String script = "(" + command + ") </dev/null\nprintf '%s%d\\n' '" + marker + "' $?\n";
            stdin.write(script);
            stdin.flush();

            List<String> lines = new ArrayList<>();
            long deadline = System.currentTimeMillis() + effectiveTimeout;
            Integer exitCode = null;
            while (System.currentTimeMillis() < deadline) {
                if (!stdout.ready()) {
                    if (!process.isAlive()) {
                        throw new IOException("bash session exited unexpectedly with code " + process.exitValue());
                    }
                    Thread.sleep(20);
                    continue;
                }
                String line = stdout.readLine();
                if (line == null) {
                    throw new IOException("bash session closed stdout unexpectedly");
                }
                if (line.startsWith(marker)) {
                    exitCode = parseExitCode(line, marker);
                    break;
                }
                lines.add(line);
            }
            if (exitCode == null) {
                destroyProcess();
                throw new BashTimeoutException(effectiveTimeout);
            }
            String output = String.join("\n", lines);
            boolean truncated = false;
            if (output.length() > maxOutputChars) {
                output = output.substring(0, maxOutputChars);
                truncated = true;
            }
            return new BashExecutionResult(exitCode, output, truncated);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            closed = true;
            destroyProcess();
        }
    }

    private void ensureAlive() throws IOException {
        if (closed) {
            throw new IOException("bash session is closed");
        }
        if (process == null || !process.isAlive()) {
            start();
        }
    }

    private void start() throws IOException {
        destroyProcess();
        ProcessBuilder builder = new ProcessBuilder("/bin/bash");
        builder.redirectErrorStream(true);
        process = builder.start();
        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        stdin.write("""
                exec 2>&1
                mkdir -p ~/.ssh
                cat > ~/.ssh/config <<'DATABUFF_SSHCFG'
                Host *
                  GSSAPIAuthentication no
                  PreferredAuthentications publickey,password
                  ConnectTimeout 10
                  StrictHostKeyChecking accept-new
                DATABUFF_SSHCFG
                chmod 600 ~/.ssh/config
                """);
        stdin.flush();
    }

    private void destroyProcess() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            try {
                process.waitFor(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        process = null;
        stdin = null;
        stdout = null;
    }

    private static long clampTimeout(long timeoutMs) {
        if (timeoutMs <= 0) {
            return BashTools.DEFAULT_TIMEOUT_MS;
        }
        return Math.min(timeoutMs, BashTools.MAX_TIMEOUT_MS);
    }

    private static int parseExitCode(String line, String marker) throws IOException {
        String suffix = line.substring(marker.length());
        try {
            return Integer.parseInt(suffix.trim());
        } catch (NumberFormatException e) {
            throw new IOException("failed to parse bash exit code from sentinel line: " + line);
        }
    }

    record BashExecutionResult(int exitCode, String output, boolean truncated) {
    }

    static final class BashTimeoutException extends IOException {
        private final long timeoutMs;

        BashTimeoutException(long timeoutMs) {
            super("command timed out after " + timeoutMs + "ms");
            this.timeoutMs = timeoutMs;
        }

        long timeoutMs() {
            return timeoutMs;
        }
    }
}
