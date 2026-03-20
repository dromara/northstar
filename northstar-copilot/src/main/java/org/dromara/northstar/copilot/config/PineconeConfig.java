package org.dromara.northstar.copilot.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author cly
 * @Description Pinecone向量数据配置文件
 * @Date 2024/8/16 16:37
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PineconeConfig {
    private String host = "https://xxx.svc.xxx.pinecone.io";
    private String key = "";

    private String upsert = "/vectors/upsert";
    private String query = "/query";
    private String delete = "/vectors/delete";
}
