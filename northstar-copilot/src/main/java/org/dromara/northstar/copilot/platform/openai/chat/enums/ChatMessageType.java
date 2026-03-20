package org.dromara.northstar.copilot.platform.openai.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/6 23:54
 */
@Getter
@AllArgsConstructor
public enum ChatMessageType {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool"),
    ;

    private final String role;

}
