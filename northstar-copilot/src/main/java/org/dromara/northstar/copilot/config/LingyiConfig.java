package org.dromara.northstar.copilot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description 零一万物大模型
 * @Date 2024/9/9 22:53
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LingyiConfig {
    private String apiHost = "https://api.lingyiwanwu.com/";
    private String apiKey = "";
    private String chatCompletionUrl = "v1/chat/completions";
}
