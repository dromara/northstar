package org.dromara.northstar.copilot.platform.openai.embedding.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * @Author cly
 * @Description embedding的处理结果
 * @Date 2024/8/7 17:30
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbeddingObject {
    /**
     * 结果下标
     */
    private Integer index;

    /**
     * embedding的处理结果，返回向量化表征的数组
     */
    private List<Float> embedding;

    /**
     * 结果类型，目前为恒为 embedding
     */
    private String object;
}
