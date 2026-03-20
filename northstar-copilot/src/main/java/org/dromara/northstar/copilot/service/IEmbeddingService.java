package org.dromara.northstar.copilot.service;

import org.dromara.northstar.copilot.platform.openai.embedding.entity.Embedding;
import org.dromara.northstar.copilot.platform.openai.embedding.entity.EmbeddingResponse;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/2 23:15
 */
public interface IEmbeddingService {

    EmbeddingResponse embedding(String baseUrl, String apiKey, Embedding embeddingReq)  throws Exception ;
    EmbeddingResponse embedding(Embedding embeddingReq)  throws Exception ;
}
