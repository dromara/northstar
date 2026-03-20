package org.dromara.northstar.copilot.platform.openai.usage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/7 17:38
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usage implements Serializable {
    @JsonProperty("prompt_tokens")
    private long promptTokens = 0L;
    @JsonProperty("completion_tokens")
    private long completionTokens = 0L;
    @JsonProperty("total_tokens")
    private long totalTokens = 0L;
}