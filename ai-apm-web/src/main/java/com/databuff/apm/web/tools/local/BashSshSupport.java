package com.databuff.apm.web.tools.local;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes remote SSH commands for the Web container (GSSAPI stalls, short LLM timeouts).
 */
final class BashSshSupport {

    static final long REMOTE_SSH_MIN_TIMEOUT_MS = 60_000L;

    private static final Pattern SSH_COMMAND = Pattern.compile("\\bssh\\b");
    private static final Pattern REMOTE_SSH = Pattern.compile(
            "\\b(?:ssh(pass)?\\s+-p\\s+|ssh\\s+(?:-o\\s+\\S+\\s+)*[^\\s@]+@)");

    private BashSshSupport() {
    }

    static boolean looksLikeRemoteSsh(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        return REMOTE_SSH.matcher(command).find();
    }

    static String normalizeRemoteSshCommand(String command) {
        if (command == null || command.isBlank() || !looksLikeRemoteSsh(command)) {
            return command == null ? "" : command.trim();
        }
        if (command.contains("GSSAPIAuthentication")) {
            return command.trim();
        }
        Matcher matcher = SSH_COMMAND.matcher(command);
        if (!matcher.find()) {
            return command.trim();
        }
        return matcher.replaceFirst(
                "ssh -o GSSAPIAuthentication=no -o PreferredAuthentications=publickey,password "
                        + "-o ConnectTimeout=10 -o StrictHostKeyChecking=no");
    }

    static long normalizeTimeoutMs(String command, long timeoutMs) {
        if (!looksLikeRemoteSsh(command)) {
            return timeoutMs;
        }
        return Math.max(timeoutMs, REMOTE_SSH_MIN_TIMEOUT_MS);
    }
}
