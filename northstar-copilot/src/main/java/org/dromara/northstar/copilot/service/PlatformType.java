package org.dromara.northstar.copilot.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/8 17:29
 */
@AllArgsConstructor
@Getter
public enum PlatformType {
    OPENAI("openai"),
    ZHIPU("zhipu"),
    DEEPSEEK("deepseek"),
    MOONSHOT("moonshot"),
    HUNYUAN("hunyuan"),
    LINGYI("lingyi"),
    OLLAMA("ollama"),
    MINIMAX("minimax"),
    BAICHUAN("baichuan"),
    ;
    private final String platform;

    public static PlatformType getPlatform(String value) {
        String target = value.toLowerCase();
        for (PlatformType platformType : PlatformType.values()) {
            if (platformType.getPlatform().equals(target)) {
                return platformType;
            }
        }
        return PlatformType.OPENAI;
    }
}
