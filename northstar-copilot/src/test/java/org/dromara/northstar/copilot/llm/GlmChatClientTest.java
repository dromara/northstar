package org.dromara.northstar.copilot.llm;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * GLM（智谱 BigModel）调用示例。
 * <p>
 * 运行前设置环境变量：{@code GLM_API_KEY}（必需）、{@code GLM_MODEL}（可选，默认 glm-4-flash）。
 * 未设置 API Key 时本测试自动跳过。
 */
@EnabledIfEnvironmentVariable(named = "GLM_API_KEY", matches = ".+")
class GlmChatClientTest extends AbstractLlmChatClientTest {

	@Override
	protected LlmProvider provider() {
		return LlmProvider.GLM;
	}
}
