package com.databuff.apm.web.tools.local;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BashSshSupportTest {

    @Test
    void injectsSshOptionsWhenMissing() {
        String command = "sshpass -p 'pwd' ssh -o StrictHostKeyChecking=no root@192.168.1.1 \"docker ps\"";
        String normalized = BashSshSupport.normalizeRemoteSshCommand(command);
        assertThat(normalized).contains("GSSAPIAuthentication=no");
        assertThat(normalized).contains("ConnectTimeout=10");
    }

    @Test
    void leavesCommandUntouchedWhenOptionsPresent() {
        String command = "ssh -o GSSAPIAuthentication=no root@host echo ok";
        assertThat(BashSshSupport.normalizeRemoteSshCommand(command)).isEqualTo(command);
    }

    @Test
    void bumpsRemoteSshTimeoutToAtLeastSixtySeconds() {
        assertThat(BashSshSupport.normalizeTimeoutMs(
                "sshpass -p x ssh user@host echo ok", 15_000L))
                .isEqualTo(BashSshSupport.REMOTE_SSH_MIN_TIMEOUT_MS);
    }

    @Test
    void keepsLocalCommandTimeout() {
        assertThat(BashSshSupport.normalizeTimeoutMs("echo ok", 15_000L)).isEqualTo(15_000L);
    }
}
