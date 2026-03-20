package org.dromara.northstar.copilot.platform.ollama.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

import java.util.List;

import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatMessage;
import org.dromara.northstar.copilot.platform.openai.tool.Tool;

/**
 * @Author cly
 * @Description Ollama对话请求实体
 * @Date 2024/9/20 0:19
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor()
@AllArgsConstructor()
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaChatCompletion {

    @NonNull
    private String model;

    @NonNull
    private List<OllamaMessage> messages;

    /**
     * 模型可能会调用的 tool 的列表。目前，仅支持 function 作为工具。使用此参数来提供以 JSON 作为输入参数的 function 列表。
     * 使用tools时，需要设置stream为false
     */
    private List<Tool> tools;

    /**
     * 辅助属性
     */
    @JsonIgnore
    private List<String> functions;

    private OllamaOptions options;

    private Boolean stream;
}
