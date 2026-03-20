package org.dromara.northstar.copilot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author : isxuwl
 * @Date: 2024/10/15 16:08
 * @Model Description:
 * @Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MinimaxConfig {
    private String apiHost = "https://api.minimax.chat/";
    private String apiKey = "";
    private String chatCompletionUrl = "v1/text/chatcompletion_v2";
}
