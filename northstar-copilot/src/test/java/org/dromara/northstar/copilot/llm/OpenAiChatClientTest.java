package org.dromara.northstar.copilot.llm;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * OpenAI 调用示例。
 * <p>
 * 运行前设置环境变量：{@code OPENAI_API_KEY}（必需）、{@code OPENAI_MODEL}（可选，默认 gpt-4o-mini）。
 * 未设置 API Key 时本测试自动跳过。
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class OpenAiChatClientTest extends AbstractLlmChatClientTest {

	@Override
	protected LlmProvider provider() {
		return LlmProvider.OPENAI;
	}
}
