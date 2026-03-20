package org.dromara.northstar.copilot.service;

import org.dromara.northstar.copilot.config.*;
import org.dromara.northstar.copilot.websearch.searxng.SearXNGConfig;

import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSources;


/**
 * @Author cly
 * @Description 统一的配置管理
 * @Date 2024/8/8 23:44
 */

@Data
public class Configuration {

    private OkHttpClient okHttpClient;

    public EventSource.Factory createRequestFactory() {
        return EventSources.createFactory(okHttpClient);
    }

    private OpenAiConfig openAiConfig;
    private ZhipuConfig zhipuConfig;
    private DeepSeekConfig deepSeekConfig;
    private MoonshotConfig moonshotConfig;
    private HunyuanConfig hunyuanConfig;
    private LingyiConfig lingyiConfig;
    private OllamaConfig ollamaConfig;
    private MinimaxConfig minimaxConfig;
    private BaichuanConfig baichuanConfig;

    private PineconeConfig pineconeConfig;

    private SearXNGConfig searXNGConfig;
}
