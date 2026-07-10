package com.databuff.apm.ingest.skywalking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stable hex IDs for SkyWalking segment-local span integers. */
public final class SkyWalkingIdUtil {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static final ThreadLocal<MessageDigest> SHA256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    });

    private SkyWalkingIdUtil() {
    }

    public static String spanId(String traceSegmentId, int spanId) {
        if (traceSegmentId == null || traceSegmentId.isBlank()) {
            return digest("unknown:" + spanId);
        }
        return digest(traceSegmentId + ":" + spanId);
    }

    public static String digest(String input) {
        MessageDigest digest = SHA256.get();
        digest.reset();
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        char[] out = new char[32];
        for (int i = 0; i < 16; i++) {
            int value = hash[i] & 0xff;
            out[i * 2] = HEX[value >>> 4];
            out[i * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(out);
    }
}
