package org.dromara.northstar.copilot.llm;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * MiniMax 调用示例。
 * <p>
 * 运行前设置环境变量：{@code MINIMAX_API_KEY}（必需）、{@code MINIMAX_MODEL}（可选，默认 MiniMax-M2）。
 * 未设置 API Key 时本测试自动跳过。
 */
@EnabledIfEnvironmentVariable(named = "MINIMAX_API_KEY", matches = ".+")
class MiniMaxChatClientTest extends AbstractLlmChatClientTest {

	@Override
	protected LlmProvider provider() {
		return LlmProvider.MINIMAX;
	}
}
