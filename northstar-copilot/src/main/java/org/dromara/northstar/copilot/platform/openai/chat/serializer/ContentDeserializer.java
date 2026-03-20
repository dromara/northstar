package org.dromara.northstar.copilot.platform.openai.chat.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.copilot.platform.openai.chat.entity.Content;

/**
 * @Author cly
 * @Description TODO
 * @Date 2025/2/11 0:57
 */
public class ContentDeserializer extends JsonDeserializer<Content> {
    @Override
    public Content deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        if (node.isTextual()) {
            return Content.ofText(node.asText());
        } else if (node.isArray()) {
            List<Content.MultiModal> parts = new ArrayList<>();
            for (JsonNode element : node) {
                Content.MultiModal part = p.getCodec().treeToValue(element, Content.MultiModal.class);
                parts.add(part);
            }
            return Content.ofMultiModals(parts);
        }
        throw new IOException("Unsupported content format");
    }
}