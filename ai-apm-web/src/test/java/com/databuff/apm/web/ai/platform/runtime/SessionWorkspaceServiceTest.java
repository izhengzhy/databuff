package com.databuff.apm.web.ai.platform.runtime;

import com.databuff.apm.web.ai.agent.AgentRuntimeConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionWorkspaceServiceTest {

    @TempDir
    Path tempDir;

    private SessionWorkspaceService service;

    @BeforeEach
    void setUp() {
        AgentRuntimeConfig config = new AgentRuntimeConfig();
        config.setWorkspaceDir(tempDir.toString());
        service = new SessionWorkspaceService(config);
    }

    @Test
    void savesAttachmentToSessionWorkspace() throws Exception {
        String sessionId = "session-test-001";
        String content = "service,errorRate\norder,0.12";
        String dataUrl = "data:text/plain;base64,"
                + Base64.getEncoder().encodeToString(content.getBytes());

        List<SessionWorkspaceService.SavedAttachment> saved = service.saveAttachments(
                sessionId,
                Map.of("attachments", List.of(Map.of(
                        "type", "file",
                        "name", "metrics.csv",
                        "mimeType", "text/csv",
                        "size", content.length(),
                        "dataUrl", dataUrl))));

        Assertions.assertThat(saved).hasSize(1);
        assertThat(saved.get(0).relativePath()).isEqualTo("uploads/metrics.csv");
        Path savedFile = service.resolveRelativePath(sessionId, saved.get(0).relativePath());
        assertThat(Files.readString(savedFile)).isEqualTo(content);
    }

    @Test
    void enrichMessageIncludesWorkspacePaths() {
        List<SessionWorkspaceService.SavedAttachment> saved = List.of(
                new SessionWorkspaceService.SavedAttachment(
                        "metrics.csv", "file", "text/csv", 12, "uploads/metrics.csv"));

        String message = service.enrichMessage("分析这个文件", saved);

        // Model-only context: callers must persist the original user text separately.
        assertThat(message).contains("uploads/metrics.csv");
        assertThat(message).contains("readWorkspaceFile");
        assertThat(message).startsWith("分析这个文件");
    }

    @Test
    void enrichMessageNotesImagesAreVisuallyAttached() {
        List<SessionWorkspaceService.SavedAttachment> saved = List.of(
                new SessionWorkspaceService.SavedAttachment(
                        "chart.png", "image", "image/png", 128, "uploads/chart.png"));

        String message = service.enrichMessage("请描述图片", saved);

        assertThat(message).contains("uploads/chart.png");
        assertThat(message).contains("Image attachments are included in this message for visual analysis");
    }

    @Test
    void rejectsPathTraversal() {
        assertThatThrownBy(() -> service.resolveRelativePath("session-test-002", "../secret.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvesOutputWritePath() {
        assertThat(service.resolveOutputWritePath("report.csv"))
                .isEqualTo("outputs/report.csv");
        assertThat(service.resolveOutputWritePath("outputs/summary.md"))
                .isEqualTo("outputs/summary.md");
    }

    @Test
    void tracksNewOutputFiles() throws Exception {
        String sessionId = "session-test-003";
        service.ensureOutputsDir(sessionId);
        Set<String> before = service.snapshotOutputPaths(sessionId);
        Files.writeString(
                service.resolveRelativePath(sessionId, "outputs/result.txt"),
                "hello");

        assertThat(service.listNewOutputFiles(sessionId, before))
                .extracting(SessionWorkspaceService.WorkspaceFileInfo::relativePath)
                .containsExactly("outputs/result.txt");
    }

    @Test
    void allowsDownloadFromOutputsAndUploads() throws Exception {
        String sessionId = "session-test-004";
        Files.createDirectories(service.uploadsDir(sessionId));
        Path upload = service.uploadsDir(sessionId).resolve("note.txt");
        Files.writeString(upload, "upload");

        assertThat(service.resolveDownloadPath(sessionId, "uploads/note.txt")).isEqualTo(upload);
    }

    @Test
    void rejectsDownloadOutsideAllowedPrefixes() {
        assertThatThrownBy(() -> service.resolveDownloadPath("session-test-005", "secret.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvesSkillResourceTemplates(@TempDir Path skillsDir) throws Exception {
        Path skillDir = skillsDir.resolve("skill.summary.html");
        Files.createDirectories(skillDir.resolve("templates"));
        Files.writeString(skillDir.resolve("SKILL.md"), "---\nname: skill.summary.html\n---\nbody");
        Files.writeString(skillDir.resolve("templates/summary-brief.html"), "<html>brief</html>");

        AgentRuntimeConfig config = new AgentRuntimeConfig();
        config.setWorkspaceDir(tempDir.toString());
        config.setBuiltinSkillsDir(skillsDir.toString());
        config.setCustomSkillsDir(tempDir.resolve("custom-skills").toString());
        SessionWorkspaceService resourceService = new SessionWorkspaceService(config);

        Path template = resourceService.resolveRelativePath(
                "session-test-006",
                "resources/skill.summary.html/templates/summary-brief.html");
        assertThat(Files.readString(template)).contains("<html>brief</html>");
        assertThat(resourceService.isResourcesPath("resources/skill.summary.html/templates")).isTrue();
        assertThatThrownBy(() -> resourceService.resolveOutputWritePath(
                "resources/skill.summary.html/templates/x.html"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
