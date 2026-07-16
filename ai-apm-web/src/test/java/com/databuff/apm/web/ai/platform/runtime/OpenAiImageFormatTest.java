package com.databuff.apm.web.ai.platform.runtime;

import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.extensions.model.openai.dto.OpenAIContentPart;
import io.agentscope.extensions.model.openai.dto.OpenAIMessage;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiImageFormatTest {

    private static final byte[] TINY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Test
    void openAiFormatterConvertsImageBlockToImageUrlPart() {
        ImageBlock imageBlock = ImageBlock.builder()
                .source(Base64Source.builder()
                        .mediaType("image/png")
                        .data(Base64.getEncoder().encodeToString(TINY_PNG))
                        .build())
                .build();
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .content(
                        TextBlock.builder().text("describe this image").build(),
                        imageBlock)
                .build();

        List<OpenAIMessage> formatted = new OpenAIChatFormatter().format(List.of(msg));

        assertThat(formatted).hasSize(1);
        Object content = formatted.get(0).getContent();
        assertThat(content).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<OpenAIContentPart> parts = (List<OpenAIContentPart>) content;
        assertThat(parts).anySatisfy(part -> {
            assertThat(part.getType()).isEqualTo("image_url");
            assertThat(part.getImageUrl()).isNotNull();
            assertThat(part.getImageUrl().getUrl()).startsWith("data:image/png;base64,");
        });
        assertThat(parts).anySatisfy(part -> assertThat(part.getType()).isEqualTo("text"));
    }
}
