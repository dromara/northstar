package org.dromara.northstar.copilot.platform.openai.chat.entity;

import java.util.List;

import org.dromara.northstar.copilot.platform.openai.chat.enums.ChatMessageType;
import org.dromara.northstar.copilot.platform.openai.tool.ToolCall;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/3 18:14
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {
    private Content content;
    private String role;
    private String name;
    private String refusal;

    @JsonProperty("tool_call_id")
    private String toolCallId;

    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    public ChatMessage(String userMessage) {
        this.role = ChatMessageType.USER.getRole();
        this.content = Content.ofText(userMessage);
    }
    public ChatMessage(ChatMessageType role, String message) {
        this.role = role.getRole();
        this.content = Content.ofText(message);
    }
    public ChatMessage(String role, String message) {
        this.role = role;
        this.content = Content.ofText(message);
    }

    public static ChatMessage withSystem(String content) {
        return new ChatMessage(ChatMessageType.SYSTEM, content);
    }

    public static ChatMessage withUser(String content) {
        return new ChatMessage(ChatMessageType.USER, content);
    }
    public static ChatMessage withUser(String content, String ...images) {
        return ChatMessage.builder()
                .role(ChatMessageType.USER.getRole())
                .content(Content.ofMultiModals(Content.MultiModal.withMultiModal(content, images)))
                .build();
    }

    public static ChatMessage withAssistant(String content) {
        return new ChatMessage(ChatMessageType.ASSISTANT, content);
    }
    public static ChatMessage withAssistant(List<ToolCall> toolCalls) {
        return ChatMessage.builder()
                .role(ChatMessageType.ASSISTANT.getRole())
                .toolCalls(toolCalls)
                .build();
    }

    public static ChatMessage withTool(String content, String toolCallId) {
        return ChatMessage.builder()
                .role(ChatMessageType.TOOL.getRole())
                .content(Content.ofText(content))
                .toolCallId(toolCallId)
                .build();
    }

}
