package org.dromara.northstar.copilot.platform.openai.embedding;

import org.dromara.northstar.copilot.config.OpenAiConfig;
import org.dromara.northstar.copilot.constant.Constants;
import org.dromara.northstar.copilot.platform.openai.embedding.entity.Embedding;
import org.dromara.northstar.copilot.platform.openai.embedding.entity.EmbeddingResponse;
import org.dromara.northstar.copilot.service.Configuration;
import org.dromara.northstar.copilot.service.IEmbeddingService;
import org.dromara.northstar.copilot.utils.ValidateUtil;

import com.alibaba.fastjson2.JSON;

import okhttp3.*;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/7 17:40
 */
public class OpenAiEmbeddingService implements IEmbeddingService {

    private final OpenAiConfig openAiConfig;
    private final OkHttpClient okHttpClient;

    public OpenAiEmbeddingService(Configuration configuration) {
        this.openAiConfig = configuration.getOpenAiConfig();
        this.okHttpClient = configuration.getOkHttpClient();
    }


    @Override
    public EmbeddingResponse embedding(String baseUrl, String apiKey, Embedding embeddingReq)  throws Exception  {
        if(baseUrl == null || "".equals(baseUrl)) baseUrl = openAiConfig.getApiHost();
        if(apiKey == null || "".equals(apiKey)) apiKey = openAiConfig.getApiKey();
        String jsonString = JSON.toJSONString(embeddingReq);

        Request request = new Request.Builder()
                .header("Authorization", "Bearer " + apiKey)
                .url(ValidateUtil.concatUrl(baseUrl, openAiConfig.getEmbeddingUrl()))
                .post(RequestBody.create(MediaType.parse(Constants.APPLICATION_JSON), jsonString))
                .build();
        Response execute = okHttpClient.newCall(request).execute();
        if (execute.isSuccessful() && execute.body() != null) {
            return JSON.parseObject(execute.body().string(), EmbeddingResponse.class);
        }
        return null;
    }

    @Override
    public EmbeddingResponse embedding(Embedding embeddingReq) throws Exception {
        return embedding(null, null, embeddingReq);
    }
}
