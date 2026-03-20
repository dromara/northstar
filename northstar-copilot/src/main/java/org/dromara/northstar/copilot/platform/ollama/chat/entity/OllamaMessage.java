package org.dromara.northstar.copilot.platform.ollama.chat.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.dromara.northstar.copilot.platform.openai.tool.ToolCall;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/9/20 0:25
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaMessage {
    private String role;
    private String content;
    private List<String> images;

    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;
}
