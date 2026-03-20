package org.dromara.northstar.copilot.platform.moonshot.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.dromara.northstar.copilot.platform.openai.chat.entity.Choice;
import org.dromara.northstar.copilot.platform.openai.usage.Usage;

/**
 * @Author cly
 * @Description 月之暗面对话响应实体
 * @Date 2024/8/29 10:28
 */

@Data
@NoArgsConstructor()
@AllArgsConstructor()
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MoonshotChatCompletionResponse {
    /**
     * 该对话的唯一标识符。
     */
    private String id;

    /**
     * 对象的类型, 其值为 chat.completion 或 chat.completion.chunk
     */
    private String object;

    /**
     * 创建聊天完成时的 Unix 时间戳（以秒为单位）。
     */
    private Long created;

    /**
     * 生成该 completion 的模型名。
     */
    private String model;

    /**
     * 模型生成的 completion 的选择列表。
     */
    private List<Choice> choices;

    /**
     * 该对话补全请求的用量信息。
     */
    private Usage usage;

    /**
     * 该指纹代表模型运行时使用的后端配置。
     */
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;
}
