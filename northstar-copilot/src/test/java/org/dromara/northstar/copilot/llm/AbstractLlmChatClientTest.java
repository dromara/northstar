package org.dromara.northstar.copilot.llm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import reactor.core.publisher.Flux;

/**
 * 各平台大模型调用示例的抽象基类。
 * <p>
 * 子类只需指定平台；未配置对应平台的 API Key 环境变量时，子类测试会自动跳过，
 * 因此该基类同时演示了三种典型调用方式：同步调用、流式调用、获取完整响应元数据。
 */
abstract class AbstractLlmChatClientTest {

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
		String content = chatClient.prompt()
				.system("你是一个量化交易助手，回答务必简洁")
				.user("用一句话解释什么是移动平均线")
				.call()
				.content();

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

		List<String> chunks = stream.collectList().block();
		assertNotNull(chunks);
		assertFalse(chunks.isEmpty());

		String content = String.join("", chunks);
		System.out.printf("[%s] 流式回复（共%d段）：%s%n", provider().getDisplayName(), chunks.size(), content);
		assertFalse(content.isBlank());
	}

	/**
	 * 完整响应示例：可获取模型名、Token 用量等元数据
	 */
	@Test
	void testChatResponse() {
		ChatResponse response = chatClient.prompt()
				.user("1+1等于几？只回答数字")
				.call()
				.chatResponse();

		assertNotNull(response.getResult());
		assertNotNull(response.getResult().getOutput().getText());
		System.out.printf("[%s] 回复：%s，Token用量：%s%n",
				provider().getDisplayName(),
				response.getResult().getOutput().getText(),
				response.getMetadata().getUsage());
	}
}
