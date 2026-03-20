package org.dromara.northstar.copilot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description 月之暗面配置
 * @Date 2024/8/29 23:00
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoonshotConfig {
    private String apiHost = "https://api.moonshot.cn/";
    private String apiKey = "";
    private String chatCompletionUrl = "v1/chat/completions";
    private String cachingUrl = "v1/caching";
}
