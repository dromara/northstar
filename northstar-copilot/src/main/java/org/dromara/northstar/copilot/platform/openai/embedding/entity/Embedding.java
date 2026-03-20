package org.dromara.northstar.copilot.platform.openai.embedding.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * @Author cly
 * @Description Embedding 实体类
 * @Date 2024/8/7 17:20
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Embedding {
    /**
     * 向量化文本
     */
    @NonNull
    private Object input;

    /**
     * 向量模型
     */
    @NonNull
    @Builder.Default
    private String model = "text-embedding-3-small";
    @JsonProperty("encoding_format")
    private String encodingFormat;

    /**
     * 向量维度 建议选择256、512、1024或2048维度
     */
    private String dimensions;
    private String user;

    public static class EmbeddingBuilder {
        private Object input;
        private Embedding.EmbeddingBuilder input(Object input){
            this.input = input;
            return this;
        }

        public Embedding.EmbeddingBuilder input(String input){
            this.input = input;
            return this;
        }

        public Embedding.EmbeddingBuilder input(List<String> content){
            this.input = content;
            return this;
        }


    }
}
