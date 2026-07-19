package com.databuff.apm.web.ai.agent;

import com.databuff.apm.web.ai.platform.runtime.SessionWorkspaceService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/ai/sessions/{sessionId}/workspace")
public class SessionWorkspaceController {

    private final SessionWorkspaceService sessionWorkspaceService;

    public SessionWorkspaceController(SessionWorkspaceService sessionWorkspaceService) {
        this.sessionWorkspaceService = sessionWorkspaceService;
    }

    @GetMapping("/files")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String sessionId,
            @RequestParam("path") String path,
            @RequestParam(value = "preview", required = false, defaultValue = "false") boolean preview,
            @RequestParam(value = "embed", required = false, defaultValue = "false") boolean embed) {
        Path file = sessionWorkspaceService.resolveDownloadPath(sessionId, path);
        String fileName = file.getFileName() == null ? "download" : file.getFileName().toString();
        Resource resource = new FileSystemResource(file);
        boolean inline = preview || embed;
        if (inline && isHtmlFile(fileName)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        }
        if (inline) {
            MediaType mediaType = probeInlineMediaType(fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(mediaType)
                    .body(resource);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private static boolean isHtmlFile(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".html") || lower.endsWith(".htm");
    }

    private static MediaType probeInlineMediaType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".md") || lower.endsWith(".txt") || lower.endsWith(".log") || lower.endsWith(".csv")) {
            return new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8);
        }
        if (lower.endsWith(".markdown")) {
            return new MediaType("text", "markdown", java.nio.charset.StandardCharsets.UTF_8);
        }
        if (lower.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        }
        if (lower.endsWith(".svg")) {
            return MediaType.parseMediaType("image/svg+xml");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
