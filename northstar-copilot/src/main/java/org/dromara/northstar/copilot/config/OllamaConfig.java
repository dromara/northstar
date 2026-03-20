package org.dromara.northstar.copilot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description Ollama配置文件
 * @Date 2024/9/20 11:07
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OllamaConfig {
    private String apiHost = "http://localhost:11434/";
    private String apiKey = "";
    private String chatCompletionUrl = "api/chat";
    private String embeddingUrl = "api/embed";
}
