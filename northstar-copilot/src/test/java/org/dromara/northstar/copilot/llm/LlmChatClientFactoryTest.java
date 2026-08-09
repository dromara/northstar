package org.dromara.northstar.copilot.llm;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

/**
 * {@link LlmChatClientFactory} 的离线单元测试（不发起真实网络请求）。
 */
class LlmChatClientFactoryTest {

	/**
	 * 显式传入 API Key 时，无需环境变量即可构建 ChatClient（构建过程不触网）
	 */
	@Test
	void testCreateWithExplicitApiKey() {
		ChatClient client = LlmChatClientFactory.create(LlmProvider.OPENAI, "sk-test-dummy");
		assertNotNull(client);
	}

	/**
	 * API Key 为空时应抛出异常，并提示对应的环境变量名
	 */
	@Test
	void testMissingApiKey() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> LlmChatClientFactory.create(LlmProvider.OPENAI, " "));
		assertTrue(ex.getMessage().contains("OPENAI_API_KEY"));
	}

	/**
	 * 所有平台都必须具备合法的接入配置
	 */
	@Test
	void testAllProvidersWellConfigured() {
		for (LlmProvider provider : LlmProvider.values()) {
			assertTrue(provider.getBaseUrl().startsWith("https://"), provider + " baseUrl 非法");
			assertNotNull(provider.getDefaultModel(), provider + " 缺少默认模型");
			assertNotNull(provider.getApiKeyEnv(), provider + " 缺少 API Key 环境变量名");
		}
	}
}
