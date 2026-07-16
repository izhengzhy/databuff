package com.databuff.apm.web.ai.platform.runtime;

import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.TextBlock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds AgentScope vision blocks from session workspace image files.
 */
final class WorkspaceImageSupport {

    static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private WorkspaceImageSupport() {
    }

    static boolean isImageAttachment(Map<?, ?> attachment) {
        if (attachment == null || attachment.isEmpty()) {
            return false;
        }
        String type = stringValue(attachment.get("type"));
        if ("image".equalsIgnoreCase(type)) {
            return true;
        }
        return isImageMimeType(stringValue(attachment.get("mimeType")));
    }

    static boolean isImageMimeType(String mimeType) {
        return mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    static boolean isImageFile(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            return false;
        }
        String probed = Files.probeContentType(file);
        if (isImageMimeType(probed)) {
            return true;
        }
        return detectImageMimeType(readHeader(file, 16)) != null
                || isImageMimeType(guessMimeTypeFromName(file.getFileName().toString()));
    }

    static List<ImageBlock> readImageAttachments(SessionWorkspaceService workspaceService, String sessionId, Object raw) {
        if (workspaceService == null || sessionId == null || sessionId.isBlank() || !(raw instanceof List<?> items)) {
            return List.of();
        }
        List<ImageBlock> blocks = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> attachment) || !isImageAttachment(attachment)) {
                continue;
            }
            String relativePath = stringValue(attachment.get("filePath"));
            if (relativePath == null || relativePath.isBlank()) {
                continue;
            }
            readImageBlock(workspaceService, sessionId, relativePath).ifPresent(blocks::add);
        }
        return List.copyOf(blocks);
    }

    static java.util.Optional<ImageBlock> readImageBlock(
            SessionWorkspaceService workspaceService,
            String sessionId,
            String relativePath) {
        try {
            Path file = workspaceService.resolveRelativePath(sessionId, relativePath);
            if (!isImageFile(file)) {
                return java.util.Optional.empty();
            }
            long size = Files.size(file);
            if (size <= 0 || size > MAX_IMAGE_BYTES) {
                return java.util.Optional.empty();
            }
            byte[] bytes = Files.readAllBytes(file);
            String mimeType = detectImageMimeType(readHeader(file, 16));
            if (mimeType == null) {
                mimeType = Files.probeContentType(file);
            }
            if (mimeType == null) {
                mimeType = guessMimeTypeFromName(file.getFileName().toString());
            }
            if (!isImageMimeType(mimeType)) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(ImageBlock.builder()
                    .source(Base64Source.builder()
                            .mediaType(mimeType)
                            .data(Base64.getEncoder().encodeToString(bytes))
                            .build())
                    .build());
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    static List<ContentBlock> buildUserContentBlocks(String text, List<ImageBlock> imageBlocks) {
        List<ContentBlock> blocks = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            blocks.add(TextBlock.builder().text(text.trim()).build());
        }
        blocks.addAll(imageBlocks);
        return List.copyOf(blocks);
    }

    private static byte[] readHeader(Path file, int length) throws IOException {
        byte[] header = new byte[length];
        try (var input = Files.newInputStream(file)) {
            int read = input.read(header);
            if (read <= 0) {
                return new byte[0];
            }
            if (read == header.length) {
                return header;
            }
            byte[] trimmed = new byte[read];
            System.arraycopy(header, 0, trimmed, 0, read);
            return trimmed;
        }
    }

    private static String detectImageMimeType(byte[] header) {
        if (header.length >= 8
                && header[0] == (byte) 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47) {
            return "image/png";
        }
        if (header.length >= 3
                && header[0] == (byte) 0xFF
                && header[1] == (byte) 0xD8
                && header[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (header.length >= 6
                && header[0] == 'G'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == '8') {
            return "image/gif";
        }
        if (header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static String guessMimeTypeFromName(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
