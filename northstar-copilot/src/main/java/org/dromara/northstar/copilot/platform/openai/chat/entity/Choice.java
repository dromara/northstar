package org.dromara.northstar.copilot.platform.openai.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @Author cly
 * @Description 模型生成的 completion
 * @Date 2024/8/11 20:01
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Choice {
    private Integer index;

    private ChatMessage delta;
    private ChatMessage message;

    private Object logprobs;

    /**
     * 模型停止生成 token 的原因。
     *
     * [stop, length, content_filter, tool_calls, insufficient_system_resource]
     *
     * stop：模型自然停止生成，或遇到 stop 序列中列出的字符串。
     * length：输出长度达到了模型上下文长度限制，或达到了 max_tokens 的限制。
     * content_filter：输出内容因触发过滤策略而被过滤。
     * tool_calls：函数调用。
     * insufficient_system_resource：系统推理资源不足，生成被打断。
     *
     */
    @JsonProperty("finish_reason")
    private String finishReason;
}
