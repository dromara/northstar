package org.dromara.northstar.copilot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description DeepSeek 配置文件
 * @Date 2024/8/29 10:31
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekConfig {

    private String apiHost = "https://api.deepseek.com/";
    private String apiKey = "";
    private String chatCompletionUrl = "chat/completions";
}
