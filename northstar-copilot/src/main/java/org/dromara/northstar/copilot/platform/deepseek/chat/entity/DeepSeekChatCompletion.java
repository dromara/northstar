package org.dromara.northstar.copilot.platform.deepseek.chat.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dromara.northstar.copilot.platform.openai.chat.entity.ChatMessage;
import org.dromara.northstar.copilot.platform.openai.chat.entity.StreamOptions;
import org.dromara.northstar.copilot.platform.openai.tool.Tool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @Author cly
 * @Description DeepSeek对话请求实体
 * @Date 2024/8/29 10:27
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor()
@AllArgsConstructor()
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepSeekChatCompletion {


    @NonNull
    private String model;

    @NonNull
    private List<ChatMessage> messages;

    /**
     * 介于 -2.0 和 2.0 之间的数字。如果该值为正，那么新 token 会根据其在已有文本中的出现频率受到相应的惩罚，降低模型重复相同内容的可能性。
     */
    @Builder.Default
    @JsonProperty("frequency_penalty")
    private Float frequencyPenalty = 0f;

    /**
     * 限制一次请求中模型生成 completion 的最大 token 数。输入 token 和输出 token 的总长度受模型的上下文长度的限制。
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 介于 -2.0 和 2.0 之间的数字。如果该值为正，那么新 token 会根据其是否已在已有文本中出现受到相应的惩罚，从而增加模型谈论新主题的可能性。
     */
    @Builder.Default
    @JsonProperty("presence_penalty")
    private Float presencePenalty = 0f;

    /**
     * 一个 object，指定模型必须输出的格式。
     *
     * 设置为 { "type": "json_object" } 以启用 JSON 模式，该模式保证模型生成的消息是有效的 JSON。
     *
     * 注意: 使用 JSON 模式时，你还必须通过系统或用户消息指示模型生成 JSON。
     * 否则，模型可能会生成不断的空白字符，直到生成达到令牌限制，从而导致请求长时间运行并显得“卡住”。
     * 此外，如果 finish_reason="length"，这表示生成超过了 max_tokens 或对话超过了最大上下文长度，消息内容可能会被部分截断。
     */
    @JsonProperty("response_format")
    private Object responseFormat;

    /**
     * 在遇到这些词时，API 将停止生成更多的 token。
     */
    private List<String> stop;

    /**
     * 如果设置为 True，将会以 SSE（server-sent events）的形式以流式发送消息增量。消息流以 data: [DONE] 结尾
     */
    @Builder.Default
    private Boolean stream = false;

    /**
     * 流式输出相关选项。只有在 stream 参数为 true 时，才可设置此参数。
     */
    @Builder.Default
    @JsonProperty("stream_options")
    private StreamOptions streamOptions = new StreamOptions();

    /**
     * 采样温度，介于 0 和 2 之间。更高的值，如 0.8，会使输出更随机，而更低的值，如 0.2，会使其更加集中和确定。
     * 我们通常建议可以更改这个值或者更改 top_p，但不建议同时对两者进行修改。
     */
    @Builder.Default
    private Float temperature = 1f;

    /**
     * 作为调节采样温度的替代方案，模型会考虑前 top_p 概率的 token 的结果。所以 0.1 就意味着只有包括在最高 10% 概率中的 token 会被考虑。
     * 我们通常建议修改这个值或者更改 temperature，但不建议同时对两者进行修改。
     */
    @Builder.Default
    @JsonProperty("top_p")
    private Float topP = 1f;

    /**
     * 模型可能会调用的 tool 的列表。目前，仅支持 function 作为工具。使用此参数来提供以 JSON 作为输入参数的 function 列表。
     */
    private List<Tool> tools;

    /**
     * 辅助属性
     */
    @JsonIgnore
    private List<String> functions;

    /**
     * 控制模型调用 tool 的行为。
     * none 意味着模型不会调用任何 tool，而是生成一条消息。
     * auto 意味着模型可以选择生成一条消息或调用一个或多个 tool。
     * 当没有 tool 时，默认值为 none。如果有 tool 存在，默认值为 auto。
     */
    @JsonProperty("tool_choice")
    private String toolChoice;

    /**
     * 是否返回所输出 token 的对数概率。如果为 true，则在 message 的 content 中返回每个输出 token 的对数概率。
     */
    @Builder.Default
    private Boolean logprobs = false;

    /**
     * 一个介于 0 到 20 之间的整数 N，指定每个输出位置返回输出概率 top N 的 token，且返回这些 token 的对数概率。指定此参数时，logprobs 必须为 true。
     */
    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    public static class DeepSeekChatCompletionBuilder {
        private List<String> functions;

        public DeepSeekChatCompletion.DeepSeekChatCompletionBuilder functions(String... functions){
            if (this.functions == null) {
                this.functions = new ArrayList<>();
            }
            this.functions.addAll(Arrays.asList(functions));
            return this;
        }

        public DeepSeekChatCompletion.DeepSeekChatCompletionBuilder functions(List<String> functions){
            if (this.functions == null) {
                this.functions = new ArrayList<>();
            }
            this.functions.addAll(functions);
            return this;
        }


    }
}
