package org.dromara.northstar.copilot.platform.openai.embedding.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

import java.util.List;

import org.dromara.northstar.copilot.platform.openai.usage.Usage;

/**
 * @Author cly
 * @Description Embedding接口的返回结果
 * @Date 2024/8/7 17:44
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmbeddingResponse {
    private String object;
    private List<EmbeddingObject> data;
    private String model;
    private Usage usage;
}
