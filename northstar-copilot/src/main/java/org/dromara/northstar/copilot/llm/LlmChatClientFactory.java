package org.dromara.northstar.copilot.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.util.Assert;

/**
 * 基于 Spring AI 的大模型 {@link ChatClient} 工厂。
 * <p>
 * Kimi / DeepSeek / MiniMax / GLM / OpenAI 均提供 OpenAI 兼容接口，
 * 统一使用 Spring AI 的 OpenAI ChatModel 接入，按平台切换 baseUrl / apiKey / model 即可。
 * <p>
 * 典型用法：
 * <pre>{@code
 * ChatClient client = LlmChatClientFactory.create(LlmProvider.DEEPSEEK);
 * String answer = client.prompt().user("你好").call().content();
 * }</pre>
 * 注意：默认不下发 temperature 等采样参数，由各平台使用自身默认值
 * （部分模型如 Kimi K2 仅允许 temperature=1，显式传其他值会报 400）。
 */
public final class LlmChatClientFactory {

	private LlmChatClientFactory() {
	}

	/**
	 * 使用环境变量中配置的 API Key 与模型创建 ChatClient
	 * @param provider 大模型平台
	 * @return ChatClient
	 */
	public static ChatClient create(LlmProvider provider) {
		Assert.notNull(provider, "provider 不能为空");
		return create(provider, provider.resolveApiKey(), provider.resolveModel());
	}

	/**
	 * 使用指定的 API Key 与平台默认模型创建 ChatClient
	 * @param provider 大模型平台
	 * @param apiKey API Key
	 * @return ChatClient
	 */
	public static ChatClient create(LlmProvider provider, String apiKey) {
		Assert.notNull(provider, "provider 不能为空");
		return create(provider, apiKey, provider.getDefaultModel());
	}

	/**
	 * 使用指定的 API Key 与模型创建 ChatClient
	 * @param provider 大模型平台
	 * @param apiKey API Key
	 * @param model 模型名
	 * @return ChatClient
	 */
	public static ChatClient create(LlmProvider provider, String apiKey, String model) {
		return ChatClient.create(createChatModel(provider, apiKey, model, null));
	}

	/**
	 * 使用指定的 API Key、模型与采样温度创建 ChatClient
	 * @param provider 大模型平台
	 * @param apiKey API Key
	 * @param model 模型名
	 * @param temperature 采样温度；传 null 表示不下发该参数（推荐，部分模型仅允许特定取值）
	 * @return ChatClient
	 */
	public static ChatClient create(LlmProvider provider, String apiKey, String model, Double temperature) {
		return ChatClient.create(createChatModel(provider, apiKey, model, temperature));
	}

	/**
	 * 创建底层的 {@link ChatModel}（需要更细粒度控制时可直接操作该对象）
	 * @param provider 大模型平台
	 * @param apiKey API Key
	 * @param model 模型名
	 * @param temperature 采样温度；传 null 表示不下发该参数
	 * @return ChatModel
	 */
	public static ChatModel createChatModel(LlmProvider provider, String apiKey, String model, Double temperature) {
		Assert.notNull(provider, "provider 不能为空");
		Assert.hasText(apiKey, () -> "未配置 API Key，请设置环境变量 " + provider.getApiKeyEnv());
		Assert.hasText(model, "model 不能为空");
		OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder()
				.baseUrl(provider.getBaseUrl())
				.apiKey(apiKey)
				.model(model);
		if (temperature != null) {
			builder.temperature(temperature);
		}
		return OpenAiChatModel.builder()
				.options(builder.build())
				.build();
	}
}
