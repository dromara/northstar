package org.dromara.northstar.copilot.platform.orcarouter.chat;

import org.dromara.northstar.copilot.config.OrcaRouterConfig;
import org.dromara.northstar.copilot.platform.openai.chat.OpenAiChatService;
import org.dromara.northstar.copilot.service.Configuration;
import org.dromara.northstar.copilot.service.PlatformType;

/**
 * @Description OrcaRouter 路由网关（OpenAI 兼容，https://www.orcarouter.ai）聊天服务
 */
public class OrcaRouterChatService extends OpenAiChatService {

    public OrcaRouterChatService(Configuration configuration) {
        super(configuration.getOrcaRouterConfig(), configuration);
    }

    @Override
    public PlatformType platformType() {
        return PlatformType.ORCA_ROUTER;
    }
}
