package org.dromara.northstar.copilot.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.dromara.northstar.copilot.tool.ToolLoader;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

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
	 * 离线验证自定义工具的注册：无需 API Key、不触网。
	 * <p>
	 * {@code ToolCallbacks.from(...)} 会扫描工具对象上的 {@code @Tool} 注解方法，
	 * 生成可供大模型识别的工具元数据（名称、描述、参数 Schema），并且可以直接在本地执行。
	 */
	@Test
	void testToolCallbackRegistration() {
		ToolCallback[] callbacks = ToolCallbacks.from(new ExchangeTools());
		assertEquals(2, callbacks.length, "ExchangeTools 中的两个 @Tool 方法都应被注册");

		// 工具名默认为方法名，工具描述来自 @Tool 注解的 description
		ToolCallback callback = Arrays.stream(callbacks)
				.filter(c -> "getCommissionRate".equals(c.getToolDefinition().name()))
				.findFirst()
				.orElseThrow();
		assertTrue(callback.getToolDefinition().description().contains("手续费"));
		assertTrue(callback.getToolDefinition().inputSchema().contains("exchange"));

		// 模拟大模型发起的工具调用：入参为 JSON 字符串，返回值即工具方法的执行结果
		String result = callback.call("{\"exchange\":\"SHFE\"}");
		assertEquals("3.0", result);
	}

	/**
	 * 离线验证工具的动态加载：ToolLoader 应能从包扫描中发现 @CopilotTool 标注的
	 * ExchangeTools，并提取出其中全部 @Tool 方法
	 */
	@Test
	void testDynamicToolLoading() {
		List<Object> tools = ToolLoader.load("org.dromara.northstar.copilot");
		assertTrue(tools.stream().anyMatch(ExchangeTools.class::isInstance), "动态加载应发现 ExchangeTools");

		// 提取工具对象中的 @Tool 方法，验证工具元数据
		var names = Arrays.stream(ToolCallbacks.from(tools.toArray()))
				.map(c -> c.getToolDefinition().name())
				.toList();
		assertTrue(names.contains("getCommissionRate"), "动态加载应包含 getCommissionRate");
		assertTrue(names.contains("getExchangeName"), "动态加载应包含 getExchangeName");
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
