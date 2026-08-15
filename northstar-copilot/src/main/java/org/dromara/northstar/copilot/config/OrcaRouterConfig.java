package org.dromara.northstar.copilot.config;

/**
 * @Description OrcaRouter 路由网关（OpenAI 兼容，https://www.orcarouter.ai）配置
 */
public class OrcaRouterConfig extends OpenAiConfig {

    public OrcaRouterConfig() {
        super();
        setApiHost("https://api.orcarouter.ai/");
        setChatCompletionUrl("v1/chat/completions");
    }
}
