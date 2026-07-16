package com.databuff.apm.web.ai.platform.runtime;

import com.databuff.apm.web.ai.agent.AgentRuntimeConfig;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.TextBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceImageSupportTest {

    private static final byte[] TINY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

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
    void readImageBlockBuildsBase64ImageBlock() throws Exception {
        String sessionId = "session-test-image-001";
        Files.createDirectories(service.uploadsDir(sessionId));
        Path image = service.uploadsDir(sessionId).resolve("dot.png");
        Files.write(image, TINY_PNG);

        ImageBlock block = WorkspaceImageSupport.readImageBlock(service, sessionId, "uploads/dot.png")
                .orElseThrow();

        assertThat(block.getSource()).isInstanceOf(Base64Source.class);
        Base64Source source = (Base64Source) block.getSource();
        assertThat(source.getMediaType()).isEqualTo("image/png");
        assertThat(Base64.getDecoder().decode(source.getData())).isEqualTo(TINY_PNG);
    }

    @Test
    void readImageAttachmentsLoadsUploadedImages() throws Exception {
        String sessionId = "session-test-image-002";
        Files.createDirectories(service.uploadsDir(sessionId));
        Files.write(service.uploadsDir(sessionId).resolve("chart.png"), TINY_PNG);

        List<ImageBlock> blocks = WorkspaceImageSupport.readImageAttachments(
                service,
                sessionId,
                List.of(Map.of(
                        "type", "image",
                        "name", "chart.png",
                        "mimeType", "image/png",
                        "filePath", "uploads/chart.png")));

        assertThat(blocks).hasSize(1);
    }

    @Test
    void buildUserContentBlocksPlacesTextBeforeImages() {
        ImageBlock imageBlock = ImageBlock.builder()
                .source(Base64Source.builder().mediaType("image/png").data("abc").build())
                .build();

        List<ContentBlock> blocks = WorkspaceImageSupport.buildUserContentBlocks("describe this", List.of(imageBlock));

        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0)).isInstanceOf(TextBlock.class);
        assertThat(((TextBlock) blocks.get(0)).getText()).isEqualTo("describe this");
        assertThat(blocks.get(1)).isInstanceOf(ImageBlock.class);
    }
}
