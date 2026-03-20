package org.dromara.northstar.copilot.platform.openai.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.copilot.platform.openai.chat.serializer.ContentDeserializer;

/**
 * @Author cly
 * @Description TODO
 * @Date 2025/2/11 0:46
 */
@ToString
@JsonDeserialize(using = ContentDeserializer.class)
public class Content {
    private String text;       // 纯文本时使用
    private List<MultiModal> multiModals; // 多模态时使用

    // 纯文本构造方法
    public static Content ofText(String text) {
        Content instance = new Content();
        instance.text = text;
        return instance;
    }

    // 多模态构造方法
    public static Content ofMultiModals(List<MultiModal> parts) {
        Content instance = new Content();
        instance.multiModals = parts;
        return instance;
    }

    // 序列化逻辑
    @JsonValue
    public Object toJson() {
        if (text != null) {
            return text; // 直接返回
        } else if (multiModals != null) {
            return multiModals;
        }
        throw new IllegalStateException("Invalid content state");
    }

    public String getText() { return text; }
    public List<MultiModal> getMultiModals() { return multiModals; }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MultiModal {
    	@Builder.Default
        private String type = Type.TEXT.type;
        private String text;
        @JsonProperty("image_url")
        private ImageUrl imageUrl;


        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ImageUrl {
            private String url;
        }

        @Getter
        @AllArgsConstructor
        public enum Type {
            TEXT("text", "文本类型"),
            IMAGE_URL("image_url", "图片类型，可以为url或者base64"),
            ;
            private final String type;
            private final String info;
        }

        public static List<MultiModal> withMultiModal(String text, String... imageUrl) {
            List<MultiModal> messages = new ArrayList<>();
            messages.add(new MultiModal(MultiModal.Type.TEXT.getType(), text, null));
            for (String url : imageUrl) {
                messages.add(new MultiModal(MultiModal.Type.IMAGE_URL.getType(), null, new MultiModal.ImageUrl(url)));
            }
            return messages;
        }

    }
}