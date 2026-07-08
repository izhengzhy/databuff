package com.databuff.apm.ingest.skywalking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stable hex IDs for SkyWalking segment-local span integers. */
public final class SkyWalkingIdUtil {

    private SkyWalkingIdUtil() {
    }

    public static String spanId(String traceSegmentId, int spanId) {
        if (traceSegmentId == null || traceSegmentId.isBlank()) {
            return digest("unknown:" + spanId);
        }
        return digest(traceSegmentId + ":" + spanId);
    }

    public static String digest(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
