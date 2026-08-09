package org.dromara.northstar.copilot.llm;

import lombok.Getter;

/**
 * 主流大模型平台枚举。
 * <p>
 * 以下平台均提供 OpenAI 兼容的 Chat Completions 接口，因此可统一通过
 * Spring AI 的 OpenAI ChatModel 接入，仅 baseUrl / apiKey / model 不同。
 * API Key 与模型名均可通过环境变量配置（模型名缺省时使用各平台默认模型）。
 */
@Getter
public enum LlmProvider {

	/** Kimi（月之暗面 Moonshot） */
	KIMI("Kimi", "https://api.moonshot.cn/v1", "kimi-k2.6", "KIMI_API_KEY", "KIMI_MODEL"),

	/** DeepSeek（深度求索） */
	DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-v4-flash", "DEEPSEEK_API_KEY", "DEEPSEEK_MODEL"),

	/** MiniMax（国内站点 api.minimaxi.com；国际站为 https://api.minimax.io/v1） */
	MINIMAX("MiniMax", "https://api.minimaxi.com/v1", "MiniMax-M3", "MINIMAX_API_KEY", "MINIMAX_MODEL"),

	/** GLM（智谱 BigModel，glm-4-flash 为免费模型） */
	GLM("GLM", "https://open.bigmodel.cn/api/paas/v4", "GLM-4.7-Flash", "GLM_API_KEY", "GLM_MODEL"),

	/** OpenAI */
	OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", "OPENAI_API_KEY", "OPENAI_MODEL");

	/** 平台显示名 */
	private final String displayName;

	/** OpenAI 兼容接口的基础地址 */
	private final String baseUrl;

	/** 默认模型名（可被环境变量覆盖） */
	private final String defaultModel;

	/** 配置 API Key 的环境变量名 */
	private final String apiKeyEnv;

	/** 覆盖默认模型名的环境变量名 */
	private final String modelEnv;

	LlmProvider(String displayName, String baseUrl, String defaultModel, String apiKeyEnv, String modelEnv) {
		this.displayName = displayName;
		this.baseUrl = baseUrl;
		this.defaultModel = defaultModel;
		this.apiKeyEnv = apiKeyEnv;
		this.modelEnv = modelEnv;
	}

	/**
	 * 从环境变量读取 API Key
	 * @return API Key；未配置时返回 null
	 */
	public String resolveApiKey() {
		String apiKey = System.getenv(apiKeyEnv);
		return apiKey == null || apiKey.isBlank() ? null : apiKey;
	}

	/**
	 * 解析实际使用的模型名：优先取环境变量覆盖值，缺省用平台默认模型
	 * @return 模型名
	 */
	public String resolveModel() {
		String model = System.getenv(modelEnv);
		return model == null || model.isBlank() ? defaultModel : model;
	}

	/**
	 * 该平台是否已配置 API Key（即可用）
	 * @return 可用返回 true
	 */
	public boolean isAvailable() {
		return resolveApiKey() != null;
	}
}
