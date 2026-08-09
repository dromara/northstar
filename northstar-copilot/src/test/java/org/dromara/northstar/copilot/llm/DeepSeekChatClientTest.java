package org.dromara.northstar.copilot.llm;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * DeepSeek（深度求索）调用示例。
 * <p>
 * 运行前设置环境变量：{@code DEEPSEEK_API_KEY}（必需）、{@code DEEPSEEK_MODEL}（可选，默认 deepseek-chat）。
 * 未设置 API Key 时本测试自动跳过。
 */
@EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
class DeepSeekChatClientTest extends AbstractLlmChatClientTest {

	@Override
	protected LlmProvider provider() {
		return LlmProvider.DEEPSEEK;
	}
}
