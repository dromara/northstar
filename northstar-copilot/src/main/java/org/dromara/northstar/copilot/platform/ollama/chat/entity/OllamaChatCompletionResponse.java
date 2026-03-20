package org.dromara.northstar.copilot.platform.ollama.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description ollama对话响应实体
 * @Date 2024/9/20 0:03
 */
@Data
@NoArgsConstructor()
@AllArgsConstructor()
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaChatCompletionResponse {

    private String model;

    @JsonProperty("created_at")
    private String createdAt;

    private OllamaMessage message;

    @JsonProperty("done_reason")
    private String doneReason;

    private Boolean done;

    @JsonProperty("total_duration")
    private long totalDuration;

    @JsonProperty("load_duration")
    private long loadDuration;

    @JsonProperty("prompt_eval_count")
    private long promptEvalCount;

    @JsonProperty("prompt_eval_duration")
    private long promptEvalDuration;

    @JsonProperty("eval_count")
    private long evalCount;

    @JsonProperty("eval_duration")
    private long evalDuration;
}
