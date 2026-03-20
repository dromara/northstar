package org.dromara.northstar.copilot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description 腾讯混元配置
 * @Date 2024/8/30 19:50
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HunyuanConfig {
    private String apiHost = "https://hunyuan.tencentcloudapi.com/";
    /**
     * apiKey 属于SecretId与SecretKey的拼接，格式为 {SecretId}.{SecretKey}
     */
    private String apiKey = "";
}
