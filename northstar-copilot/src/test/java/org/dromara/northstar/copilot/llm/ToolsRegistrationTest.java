package org.dromara.northstar.copilot.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.dromara.northstar.copilot.tool.ToolLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import reactor.core.publisher.Flux;

/**
 * {@code ChatClient} 工具注册 API 用法的离线演示测试（不发起真实网络请求）。
 * <p>
 * 通过一个捕获 {@link Prompt} 的假 {@link ChatModel}，在模型被调用时检查最终生效的工具列表，
 * 从而验证 {@code tools(Object...)} 对各种形态参数的分派规则，以及
 * {@code Builder.defaultTools(Object...)} 的作用域语义：
 * <ul>
 *   <li>{@code tools(...)} 注册的工具仅对当前请求生效；</li>
 *   <li>{@code defaultTools(...)} 注册的工具对同一 Builder 构建的每个请求都生效。</li>
 * </ul>
 */
class ToolsRegistrationTest {

	/**
	 * 捕获 Prompt 的假 ChatModel：不触网，仅记录最后一次请求并返回固定响应。
	 * <p>
	 * 注意：必须让 {@link #getOptions()} 返回 {@link ToolCallingChatOptions}，
	 * 因为 Spring AI 只在模型的 options 属于 ToolCallingChatOptions 时，才会把
	 * {@code tools(...)}/{@code defaultTools(...)} 注册的工具合并进 Prompt。
	 */
	static class CapturingChatModel implements ChatModel {

		Prompt capturedPrompt;

		@Override
		public ChatResponse call(Prompt prompt) {
			this.capturedPrompt = prompt;
			return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		@Override
		public ChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}
	}

	/**
	 * 演示用的第二个工具 POJO，用于验证多工具叠加
	 */
	static class GreetingTools {

		@Tool(description = "生成一句问候语")
		public String greet(@ToolParam(description = "被问候者的名字") String name) {
			return "你好，" + name;
		}
	}

	/**
	 * 演示用的第三个工具 POJO，用于验证混合传参
	 */
	static class EchoTools {

		@Tool(description = "原样返回输入的内容")
		public String echo(@ToolParam(description = "要返回的内容") String text) {
			return text;
		}
	}

	private CapturingChatModel chatModel;

	private ChatClient chatClient;

	@BeforeEach
	void setUp() {
		chatModel = new CapturingChatModel();
		chatClient = ChatClient.builder(chatModel).build();
	}

	/**
	 * 读取模型实际收到的请求中最终生效的工具名列表
	 */
	private List<String> capturedToolNames() {
		assertNotNull(chatModel.capturedPrompt, "ChatModel 应被调用");
		if (chatModel.capturedPrompt.getOptions() instanceof ToolCallingChatOptions options
				&& options.getToolCallbacks() != null) {
			return options.getToolCallbacks().stream()
					.map(tc -> tc.getToolDefinition().name())
					.toList();
		}
		return List.of();
	}

	/**
	 * 形态一：传 @Tool 注解的 POJO —— 每个 @Tool 方法生成一个 ToolCallback
	 */
	@Test
	void testToolsWithPojo() {
		chatClient.prompt().user("hi").tools(new ExchangeTools()).call().content();

		assertTrue(capturedToolNames().containsAll(List.of("getCommissionRate", "getExchangeName")),
				"POJO 中的 @Tool 方法应全部被注册");
	}

	/**
	 * 形态二：传 ToolCallback —— 直接注册，不经过注解扫描
	 */
	@Test
	void testToolsWithToolCallback() {
		ToolCallback callback = ToolCallbacks.from(new ExchangeTools())[0];

		chatClient.prompt().user("hi").tools(callback).call().content();

		assertEquals(1, capturedToolNames().size(), "只应注册传入的那一个回调");
	}

	/**
	 * 形态三：传 ToolCallbackProvider —— 注册时不解析，请求发起时才懒加载其回调
	 */
	@Test
	void testToolsWithProviderResolvedLazily() {
		AtomicInteger resolveCount = new AtomicInteger();
		ToolCallbackProvider provider = () -> {
			resolveCount.incrementAndGet();
			return ToolCallbacks.from(new ExchangeTools());
		};

		var spec = chatClient.prompt().user("hi").tools(provider);
		assertEquals(0, resolveCount.get(), "注册阶段不应解析 Provider");

		spec.call().content();
		assertEquals(1, resolveCount.get(), "请求发起时应恰好解析一次 Provider");
		assertTrue(capturedToolNames().contains("getCommissionRate"), "Provider 提供的回调应生效");
	}

	/**
	 * 形态四：传 ToolCallback 数组 —— 数组被拆开，每个元素按类型分派
	 */
	@Test
	void testToolsWithArray() {
		ToolCallback[] callbacks = ToolCallbacks.from(new ExchangeTools());

		chatClient.prompt().user("hi").tools((Object) callbacks).call().content();

		assertTrue(capturedToolNames().containsAll(List.of("getCommissionRate", "getExchangeName")),
				"数组中的每个回调都应被注册");
	}

	/**
	 * 形态五：传 Collection —— 逐个遍历，每个元素按类型分派。
	 * ToolLoader.load(...) 返回的 List 可直接传入，无需转数组。
	 */
	@Test
	void testToolsWithCollection() {
		List<Object> toolObjects = ToolLoader.load("org.dromara.northstar.copilot");

		chatClient.prompt().user("hi").tools(toolObjects).call().content();

		assertTrue(capturedToolNames().containsAll(List.of("getCommissionRate", "getExchangeName")),
				"Collection 中的工具对象应被逐个识别并注册");
	}

	/**
	 * 混合传参：POJO + ToolCallback + ToolCallbackProvider 可以混在一次调用中，
	 * 并可通过 toolContext(...) 携带上下文数据
	 */
	@Test
	void testMixedTools() {
		ToolCallback callback = ToolCallbacks.from(new GreetingTools())[0];
		ToolCallbackProvider provider = () -> ToolCallbacks.from(new EchoTools());

		chatClient.prompt()
				.user("hi")
				.tools(new ExchangeTools(), callback, provider)
				.toolContext(Map.of("tenantId", "acme"))
				.call()
				.content();

		List<String> names = capturedToolNames();
		assertTrue(names.contains("getCommissionRate"), "POJO 中的工具应生效");
		assertTrue(names.contains("greet"), "ToolCallback 应直接生效");
		assertTrue(names.contains("echo"), "Provider 懒加载出的工具应生效");
	}

	/**
	 * 同名工具不允许重复注册：无论来自哪种形态，工具名冲突都会抛出异常
	 */
	@Test
	void testDuplicateToolNamesRejected() {
		ToolCallback duplicate = ToolCallbacks.from(new ExchangeTools())[0];

		var spec = chatClient.prompt().user("hi").tools(new ExchangeTools(), duplicate);

		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> spec.call().content());
		assertTrue(ex.getMessage().contains("getCommissionRate") || ex.getMessage().contains("getExchangeName"),
				"异常信息应指出冲突的工具名");
	}

	/**
	 * 非法参数校验：null 数组、含 null 元素、没有任何 @Tool 方法的 POJO，
	 * 都会抛出 IllegalArgumentException
	 */
	@Test
	void testInvalidToolArguments() {
		var spec = chatClient.prompt().user("hi");

		assertThrows(IllegalArgumentException.class, () -> spec.tools((Object[]) null),
				"工具数组为 null 应抛异常");
		assertThrows(IllegalArgumentException.class, () -> spec.tools(new ExchangeTools(), null),
				"工具元素为 null 应抛异常");
		assertThrows(IllegalArgumentException.class, () -> spec.tools(new Object()),
				"无 @Tool 方法的 POJO 应抛异常");
	}

	/**
	 * 作用域：tools(...) 注册的工具仅对当前请求生效，不会泄漏到后续请求
	 */
	@Test
	void testRequestScopedToolsDoNotLeak() {
		chatClient.prompt().user("第一次").tools(new ExchangeTools()).call().content();
		assertFalse(capturedToolNames().isEmpty(), "第一次请求应携带工具");

		chatClient.prompt().user("第二次").call().content();
		assertTrue(capturedToolNames().isEmpty(), "请求级工具不应泄漏到后续请求");
	}

	/**
	 * Builder.defaultTools(...)：对同一 Builder 构建出的每个请求都生效，
	 * 即使请求本身没有显式注册任何工具
	 */
	@Test
	void testDefaultToolsApplyToEveryRequest() {
		ChatClient client = ChatClient.builder(chatModel)
				.defaultTools(new ExchangeTools())
				.build();

		// 第一次请求：不显式注册工具，默认工具依然生效
		client.prompt().user("第一次").call().content();
		assertTrue(capturedToolNames().containsAll(List.of("getCommissionRate", "getExchangeName")),
				"defaultTools 注册的工具应对所有请求生效");

		// 第二次请求：同样生效
		client.prompt().user("第二次").call().content();
		assertTrue(capturedToolNames().containsAll(List.of("getCommissionRate", "getExchangeName")),
				"defaultTools 注册的工具应对每个请求都生效");
	}

	/**
	 * defaultTools(...) 与请求级 tools(...) 是叠加关系：
	 * 默认工具始终在场，请求级工具按需追加
	 */
	@Test
	void testDefaultToolsMergedWithRequestTools() {
		ChatClient client = ChatClient.builder(chatModel)
				.defaultTools(new ExchangeTools())
				.build();

		client.prompt().user("hi").tools(new GreetingTools()).call().content();

		List<String> names = capturedToolNames();
		assertTrue(names.contains("greet"), "请求级工具应生效");
		assertTrue(names.contains("getCommissionRate"), "默认工具应与请求级工具叠加");
	}
}
