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
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import com.openai.errors.RateLimitException;

import reactor.core.publisher.Flux;

/**
 * 各平台大模型调用示例的抽象基类。
 * <p>
 * 子类只需指定平台；未配置对应平台的 API Key 环境变量时，子类测试会自动跳过，
 * 因此该基类同时演示了以下典型调用方式：
 * <ul>
 *   <li>同步调用：{@link #testChat()}</li>
 *   <li>流式调用：{@link #testStreamChat()}</li>
 *   <li>工具调用（Tool Calling）：{@link #testToolCall()}</li>
 *   <li>动态加载工具：{@link #testDynamicToolCall()}</li>
 *   <li>思考过程与工具调用结合：{@link #testThinkingWithToolCall()}</li>
 *   <li>单轮提问触发多轮工具调用：{@link #testMultiRoundToolCall()}</li>
 *   <li>跨请求多轮对话中的思考与工具调用：{@link #testMultiTurnThinkingAndToolCall()}</li>
 *   <li>完整响应元数据：{@link #testChatResponse()}</li>
 * </ul>
 * 关于“思考过程”的说明：支持推理的模型（如 DeepSeek Reasoner、GLM 思考模式、Kimi K2 Thinking）
 * 会把推理内容放在响应的 {@code reasoning_content} 字段中，Spring AI 会将其提取到
 * AssistantMessage 元数据的 {@code reasoningContent} 键下（见 {@link #extractThinking(ChatResponse)}）；
 * 不返回原生推理内容的模型，则通过提示词要求模型“先展示思考过程再回答”，同样可观察到思考链路。
 */
abstract class AbstractLlmChatClientTest {

	/** 限流重试的最大尝试次数（含首次调用） */
	private static final int MAX_ATTEMPTS = 4;

	/** 命中 429 限流后的退避等待（毫秒）。部分平台按分钟限制 RPM（如 Kimi 账号级 3 RPM），需要分钟级退避 */
	private static final long[] RETRY_BACKOFF_MS = { 30_000, 60_000, 90_000 };

	/** Spring AI 将模型原生推理内容存放在 AssistantMessage 元数据中的键名 */
	private static final String REASONING_CONTENT_KEY = "reasoningContent";

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
	 * 思考过程与工具调用结合示例：
	 * <p>
	 * 通过提示词要求模型“先展示思考过程，再给出结论”，同时注册工具；
	 * 模型会在思考中规划“需要先查手续费、再查全称”，随后回调本地工具，最后综合工具结果作答。
	 * 若所用模型支持原生推理（如 deepseek-reasoner），其内部推理内容会额外出现在
	 * AssistantMessage 元数据的 {@code reasoningContent} 键下，本例一并打印出来，
	 * 便于对比“提示词层面的可见思考”与“模型层面的原生推理”。
	 */
	@Test
	void testThinkingWithToolCall() {
		ChatResponse response = callWithRetry(() -> chatClient.prompt()
				.system("你是一个量化交易助手。回答任何问题前，先逐步展示你的思考过程"
						+ "（包括：打算调用哪个工具、为什么调用、拿到结果后如何推理），再给出最终结论。")
				.user("上期所(SHFE)每手手续费是多少元？它的中文全称是什么？")
				.tools(new ExchangeTools())
				.call()
				.chatResponse());

		String thinking = extractThinking(response);
		String content = response.getResult().getOutput().getText();

		System.out.printf("[%s] 原生推理内容：%s%n", provider().getDisplayName(),
				thinking.isBlank() ? "（该模型未返回原生推理内容，思考过程见正文）" : thinking);
		System.out.printf("[%s] 思考+工具调用回复：%s%n", provider().getDisplayName(), content);

		assertNotNull(content);
		assertFalse(content.isBlank());
		// 交易所全称只能来自工具返回结果，说明模型在思考后确实回调了本地工具
		assertTrue(content.contains("上海期货交易所"), "模型应结合思考调用工具，并在回答中包含交易所全称");
	}

	/**
	 * 多轮工具调用示例（单次提问内）：
	 * <p>
	 * 一次提问涉及多个未知信息时，Spring AI 内部会自动执行“模型请求工具 → 本地执行工具 →
	 * 结果回传模型 → 模型再次决策”的多轮循环，直到模型能给出最终答案。
	 * 本例要求比较三个交易所的手续费，模型需要多次调用工具（每个交易所至少一次），
	 * 通过 {@link CountingExchangeTools} 统计工具实际被调用的次数加以验证。
	 */
	@Test
	void testMultiRoundToolCall() {
		CountingExchangeTools tools = new CountingExchangeTools();

		String content = callWithRetry(() -> chatClient.prompt()
				.system("你是一个量化交易助手，回答务必简洁")
				.user("上期所(SHFE)、大商所(DCE)、郑商所(CZCE)每手手续费分别是多少元？哪家最便宜？")
				.tools(tools)
				.call()
				.content());

		System.out.printf("[%s] 多轮工具调用回复：%s%n", provider().getDisplayName(), content);
		System.out.printf("[%s] 工具共被调用 %d 次（手续费 %d 次，全称 %d 次）%n",
				provider().getDisplayName(), tools.totalCalls(), tools.commissionCalls(), tools.nameCalls());

		assertNotNull(content);
		assertFalse(content.isBlank());
		// 三个交易所的手续费均为未知信息，模型必须至少调用 3 次工具才能回答
		assertTrue(tools.commissionCalls() >= 3, "模型应针对三个交易所各调用一次手续费查询工具，实际调用 "
				+ tools.commissionCalls() + " 次");
		// 大商所(DCE)手续费 1.5 元，为三者中最低，只能来自工具返回结果
		assertTrue(content.contains("1.5"), "模型应基于工具结果得出大商所(1.5元)最便宜的结论");
	}

	/**
	 * 跨请求多轮对话示例：对话记忆 + 每轮思考 + 每轮工具调用。
	 * <p>
	 * 通过 {@link MessageChatMemoryAdvisor} 为 ChatClient 挂上会话记忆
	 * （{@link MessageWindowChatMemory} + 内存仓库），相同会话 ID 的多次请求会共享历史消息：
	 * <ol>
	 *   <li>第 1 轮询问上期所手续费，模型调用工具得到 3.0 元并记入记忆；</li>
	 *   <li>第 2 轮追问“大商所呢？比上期所便宜多少？”，模型需结合第 1 轮记忆理解“比上期所”
	 *       指代 3.0 元，并再次调用工具查询大商所手续费，最后完成减法推理。</li>
	 * </ol>
	 * 每轮都打印模型的思考过程（原生推理内容若有）与工具调用计数。
	 */
	@Test
	void testMultiTurnThinkingAndToolCall() {
		ChatMemory chatMemory = MessageWindowChatMemory.builder()
				.chatMemoryRepository(new InMemoryChatMemoryRepository())
				.maxMessages(20)
				.build();
		String conversationId = "multi-turn-demo-" + provider().name();
		CountingExchangeTools tools = new CountingExchangeTools();

		// 第 1 轮：建立上下文（上期所手续费 = 3.0 元）
		ChatResponse turn1 = callWithRetry(() -> chatClient.prompt()
				.system("你是一个量化交易助手，回答务必简洁。需要数据时先调用工具，再作答。")
				.user("上期所(SHFE)每手手续费是多少元？")
				.tools(tools)
				.advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
				.call()
				.chatResponse());
		printTurn(1, turn1, tools);

		// 第 2 轮：依赖第 1 轮记忆（“比上期所”指代 3.0 元），并再次调用工具查询大商所
		ChatResponse turn2 = callWithRetry(() -> chatClient.prompt()
				.system("你是一个量化交易助手，回答务必简洁。需要数据时先调用工具，再作答。")
				.user("大商所(DCE)呢？它比上期所每手便宜多少元？")
				.tools(tools)
				.advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
				.call()
				.chatResponse());
		printTurn(2, turn2, tools);

		String content2 = turn2.getResult().getOutput().getText();
		assertNotNull(content2);
		assertFalse(content2.isBlank());
		// 两轮各至少需要一次工具调用：第 1 轮查 SHFE(3.0)，第 2 轮查 DCE(1.5)
		assertTrue(tools.totalCalls() >= 2, "两轮对话应各触发至少一次工具调用，实际共 " + tools.totalCalls() + " 次");
		// 3.0 - 1.5 = 1.5，差额只能由“第 1 轮记忆 + 第 2 轮工具结果”推理得出
		assertTrue(content2.contains("1.5"), "模型应结合对话记忆与工具结果算出每手便宜 1.5 元");
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
	 * 提取模型的原生推理内容（思考过程）。
	 * <p>
	 * 支持推理的模型会在 OpenAI 兼容响应中返回 {@code reasoning_content} 字段，
	 * Spring AI 将其提取到 AssistantMessage 元数据的 {@code reasoningContent} 键下；
	 * 不支持推理的模型此处返回空串，思考过程需通过提示词引导体现在正文中。
	 */
	private static String extractThinking(ChatResponse response) {
		Object reasoning = response.getResult().getOutput().getMetadata().get(REASONING_CONTENT_KEY);
		return reasoning == null ? "" : reasoning.toString();
	}

	/**
	 * 打印多轮对话中某一轮的思考过程、回复与累计工具调用次数
	 */
	private void printTurn(int turn, ChatResponse response, CountingExchangeTools tools) {
		String thinking = extractThinking(response);
		System.out.printf("[%s] 第%d轮 原生推理内容：%s%n", provider().getDisplayName(), turn,
				thinking.isBlank() ? "（该模型未返回原生推理内容）" : thinking);
		System.out.printf("[%s] 第%d轮 回复：%s%n", provider().getDisplayName(), turn,
				response.getResult().getOutput().getText());
		System.out.printf("[%s] 第%d轮结束后 工具累计调用 %d 次%n", provider().getDisplayName(), turn, tools.totalCalls());
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

	/**
	 * 带调用计数的交易所工具：委托 {@link ExchangeTools} 提供数据，同时统计各工具方法被模型调用的次数，
	 * 用于验证“一次提问触发多轮工具调用”与“多轮对话中每轮都触发工具调用”。
	 */
	static class CountingExchangeTools {

		private final ExchangeTools delegate = new ExchangeTools();

		private int commissionCalls;

		private int nameCalls;

		@Tool(description = "查询指定期货交易所的每手交易手续费，返回单位为元")
		public double getCommissionRate(@ToolParam(description = "交易所代码，例如 SHFE、DCE、CZCE、CFFEX") String exchange) {
			commissionCalls++;
			return delegate.getCommissionRate(exchange);
		}

		@Tool(description = "查询指定期货交易所的中文全称")
		public String getExchangeName(@ToolParam(description = "交易所代码，例如 SHFE、DCE、CZCE、CFFEX") String exchange) {
			nameCalls++;
			return delegate.getExchangeName(exchange);
		}

		int commissionCalls() {
			return commissionCalls;
		}

		int nameCalls() {
			return nameCalls;
		}

		int totalCalls() {
			return commissionCalls + nameCalls;
		}
	}
}
