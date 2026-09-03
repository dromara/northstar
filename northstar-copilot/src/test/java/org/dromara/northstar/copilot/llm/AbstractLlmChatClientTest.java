package org.dromara.northstar.copilot.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Supplier;

import org.dromara.northstar.copilot.tool.ToolLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import com.openai.errors.RateLimitException;

import reactor.core.publisher.Flux;

/**
 * 各平台大模型调用示例的抽象基类。
 * <p>
 * 子类只需指定平台；未配置对应平台的 API Key 环境变量时，子类测试会自动跳过，
 * 因此该基类同时演示了四种典型调用方式：同步调用、流式调用、获取完整响应元数据、工具调用（Tool Calling）。
 */
abstract class AbstractLlmChatClientTest {

	/** 限流重试的最大尝试次数（含首次调用） */
	private static final int MAX_ATTEMPTS = 4;

	/** 命中 429 限流后的退避等待（毫秒）。部分平台按分钟限制 RPM（如 Kimi 账号级 3 RPM），需要分钟级退避 */
	private static final long[] RETRY_BACKOFF_MS = { 30_000, 60_000, 90_000 };

	protected ChatClient chatClient;

	/**
	 * 指定当前测试面向的大模型平台
	 * @return 平台枚举
	 */
	protected abstract LlmProvider provider();

	@BeforeEach
	void setUp() {
		// 通过工厂创建 ChatClient：API Key 与模型名默认取自环境变量
		chatClient = LlmChatClientFactory.create(provider());
	}

	/**
	 * 同步调用示例：一次性返回完整文本
	 */
	@Test
	void testChat() {
		String content = callWithRetry(() -> chatClient.prompt()
				.system("你是一个量化交易助手，回答务必简洁")
				.user("用一句话解释什么是移动平均线")
				.call()
				.content());

		System.out.printf("[%s] 同步回复：%s%n", provider().getDisplayName(), content);
		assertNotNull(content);
		assertFalse(content.isBlank());
	}

	/**
	 * 流式调用示例：以 Flux 形式逐段返回，适合 SSE 推送到前端
	 */
	@Test
	void testStreamChat() {
		Flux<String> stream = chatClient.prompt()
				.user("用一句话解释什么是止损")
				.stream()
				.content();

		List<String> chunks = callWithRetry(() -> stream.collectList().block());
		assertNotNull(chunks);
		assertFalse(chunks.isEmpty());

		String content = String.join("", chunks);
		System.out.printf("[%s] 流式回复（共%d段）：%s%n", provider().getDisplayName(), chunks.size(), content);
		assertFalse(content.isBlank());
	}

	/**
	 * 工具调用（Tool Calling）示例：将自定义工具对象交给大模型，
	 * 模型在需要时会自动回调本地工具方法，并把工具返回的结果融入回答。
	 * 自定义工具的编写方式参见 {@link ExchangeTools}。
	 */
	@Test
	void testToolCall() {
		String content = callWithRetry(() -> chatClient.prompt()
				.system("你是一个量化交易助手，回答务必简洁")
				.user("上期所(SHFE)每手手续费是多少元？它的中文全称是什么？")
				.tools(new ExchangeTools())
				.call()
				.content());

		System.out.printf("[%s] 工具调用回复：%s%n", provider().getDisplayName(), content);
		assertNotNull(content);
		assertFalse(content.isBlank());
		// 交易所全称只能来自工具返回结果，说明模型确实回调了本地工具
		assertTrue(content.contains("上海期货交易所"), "模型应调用工具并在回答中包含交易所全称");
	}

	/**
	 * 动态加载工具示例：不在代码中显式 new 工具类，而是由 ToolLoader 在运行时
	 * 扫描包下所有 @CopilotTool 标注的工具类并自动注册，适合工具数量多、插件化的场景。
	 */
	@Test
	void testDynamicToolCall() {
		String content = callWithRetry(() -> chatClient.prompt()
				.system("你是一个量化交易助手，回答务必简洁")
				.user("上期所(SHFE)每手手续费是多少元？它的中文全称是什么？")
				.tools(ToolLoader.load("org.dromara.northstar.copilot"))
				.call()
				.content());

		System.out.printf("[%s] 动态加载工具回复：%s%n", provider().getDisplayName(), content);
		assertNotNull(content);
		assertFalse(content.isBlank());
		assertTrue(content.contains("上海期货交易所"), "模型应调用动态加载的工具并包含交易所全称");
	}

	/**
	 * 完整响应示例：可获取模型名、Token 用量等元数据
	 */
	@Test
	void testChatResponse() {
		ChatResponse response = callWithRetry(() -> chatClient.prompt()
				.user("1+1等于几？只回答数字")
				.call()
				.chatResponse());

		assertNotNull(response.getResult());
		assertNotNull(response.getResult().getOutput().getText());
		System.out.printf("[%s] 回复：%s，Token用量：%s%n",
				provider().getDisplayName(),
				response.getResult().getOutput().getText(),
				response.getMetadata().getUsage());
	}

	/**
	 * 带限流重试的调用包装：各平台普遍存在限流（HTTP 429），
	 * 包括账号级 RPM 限制（如 Kimi）与模型高峰期拥堵（如 GLM 免费模型）。
	 * 底层 SDK 自带的重试退避过短，无法跨过分钟级的限流窗口，因此在测试层做长退避重试。
	 * 仅对 429 限流异常重试，其余异常直接抛出。
	 * @param action 实际的大模型调用
	 * @return 调用结果
	 */
	protected <T> T callWithRetry(Supplier<T> action) {
		for (int attempt = 1; ; attempt++) {
			try {
				return action.get();
			} catch (Exception e) {
				if (!isRateLimited(e) || attempt >= MAX_ATTEMPTS) {
					throw e;
				}
				long waitMs = RETRY_BACKOFF_MS[Math.min(attempt - 1, RETRY_BACKOFF_MS.length - 1)];
				System.out.printf("[%s] 触发限流(429)，%d 秒后进行第 %d 次重试...%n",
						provider().getDisplayName(), waitMs / 1000, attempt + 1);
				try {
					Thread.sleep(waitMs);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw e;
				}
			}
		}
	}

	/**
	 * 判断异常链中是否包含 429 限流错误
	 */
	private static boolean isRateLimited(Throwable t) {
		for (Throwable cur = t; cur != null; cur = cur.getCause()) {
			if (cur instanceof RateLimitException) {
				return true;
			}
			String msg = cur.getMessage();
			if (msg != null && msg.contains("429")) {
				return true;
			}
		}
		return false;
	}
}
