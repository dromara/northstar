package org.dromara.northstar.copilot.llm;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Kimi（月之暗面 Moonshot）调用示例。
 * <p>
 * 运行前设置环境变量：{@code KIMI_API_KEY}（必需）、{@code KIMI_MODEL}（可选，默认 kimi-k2-0905-preview）。
 * 未设置 API Key 时本测试自动跳过。
 */
@EnabledIfEnvironmentVariable(named = "KIMI_API_KEY", matches = ".+")
class KimiChatClientTest extends AbstractLlmChatClientTest {

	@Override
	protected LlmProvider provider() {
		return LlmProvider.KIMI;
	}
}
