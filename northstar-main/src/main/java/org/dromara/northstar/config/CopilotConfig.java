package org.dromara.northstar.config;

import java.util.concurrent.TimeUnit;

import org.dromara.northstar.copilot.config.DeepSeekConfig;
import org.dromara.northstar.copilot.config.MoonshotConfig;
import org.dromara.northstar.copilot.config.OrcaRouterConfig;
import org.dromara.northstar.copilot.platform.deepseek.chat.DeepSeekChatService;
import org.dromara.northstar.copilot.platform.moonshot.chat.MoonshotChatService;
import org.dromara.northstar.copilot.platform.orcarouter.chat.OrcaRouterChatService;
import org.dromara.northstar.copilot.service.IChatService;
import org.dromara.northstar.copilot.utils.SharedInfoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

@Slf4j
@Configuration
public class CopilotConfig {
	
	@Bean
	OkHttpClient httpClient() {
		return new OkHttpClient().newBuilder()
				.connectTimeout(5, TimeUnit.SECONDS)
				.readTimeout(15, TimeUnit.SECONDS)
				.build();
	}

	@Bean
	@ConditionalOnProperty("copilot.moonshot.apiKey")
	IChatService kimiService(@Value("${copilot.moonshot.apiKey}") String apiKey, OkHttpClient client) {
		log.info("创建Kimi聊天服务");
		MoonshotConfig moonCfg = new MoonshotConfig();
		moonCfg.setApiKey(apiKey);
		org.dromara.northstar.copilot.service.Configuration cfg = new org.dromara.northstar.copilot.service.Configuration();
		cfg.setOkHttpClient(client);
		cfg.setMoonshotConfig(moonCfg);
		SharedInfoUtils.setMoonshotConfig(moonCfg);
		return new MoonshotChatService(cfg);
	}
	
	@Bean
	@ConditionalOnProperty("copilot.deepseek.apiKey")
	IChatService siliconflowService(@Value("${copilot.deepseek.apiKey}") String apiKey, OkHttpClient client) {
		log.info("创建Deepseek聊天服务");
		DeepSeekConfig dsCfg = new DeepSeekConfig();
		dsCfg.setApiKey(apiKey);
		org.dromara.northstar.copilot.service.Configuration cfg = new org.dromara.northstar.copilot.service.Configuration();
		cfg.setOkHttpClient(client);
		cfg.setDeepSeekConfig(dsCfg);
		return new DeepSeekChatService(cfg);
	}

	@Bean
	@ConditionalOnProperty("copilot.orcarouter.apiKey")
	IChatService orcaRouterService(@Value("${copilot.orcarouter.apiKey}") String apiKey, OkHttpClient client) {
		log.info("创建OrcaRouter聊天服务");
		OrcaRouterConfig orcaCfg = new OrcaRouterConfig();
		orcaCfg.setApiKey(apiKey);
		org.dromara.northstar.copilot.service.Configuration cfg = new org.dromara.northstar.copilot.service.Configuration();
		cfg.setOkHttpClient(client);
		cfg.setOrcaRouterConfig(orcaCfg);
		return new OrcaRouterChatService(cfg);
	}
}
