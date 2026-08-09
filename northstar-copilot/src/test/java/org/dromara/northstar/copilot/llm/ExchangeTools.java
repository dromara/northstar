package org.dromara.northstar.copilot.llm;

import org.dromara.northstar.copilot.tool.CopilotTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 自定义工具（Tool）示例：向大模型提供期货交易所基础信息的查询能力。
 * <p>
 * 在 Spring AI 中自定义一个 Tool 只需两步：
 * <ol>
 *   <li>在普通 Java 方法上添加 {@link Tool} 注解，{@code description} 用自然语言描述工具用途，
 *       大模型将据此决定何时调用该工具；</li>
 *   <li>用 {@link ToolParam} 注解描述每个入参的含义，帮助大模型正确填参。</li>
 * </ol>
 * 使用时将工具对象注册到 ChatClient 即可：
 * <pre>{@code
 * String content = chatClient.prompt()
 *         .user("上期所每手手续费是多少？")
 *         .tools(new ExchangeTools())   // 注册自定义工具
 *         .call()
 *         .content();
 * }</pre>
 * 注意：本类返回的是演示用的假数据，真实场景中可在方法体内查询数据库或调用外部服务。
 * <p>
 * 类上的 {@link CopilotTool} 注解用于支持运行时动态加载（见 {@code ToolLoader} 与
 * {@code AbstractLlmChatClientTest#testDynamicToolCall()}）；如果只是在代码里显式
 * {@code new ExchangeTools()} 注册，则不需要该注解。
 */
@CopilotTool
public class ExchangeTools {

	/**
	 * 查询交易所每手交易手续费（示例数据，非真实费率）
	 * @param exchange 交易所代码，如 SHFE、DCE、CZCE、CFFEX
	 * @return 每手手续费（元），未知交易所返回 -1
	 */
	@Tool(description = "查询指定期货交易所的每手交易手续费，返回单位为元")
	public double getCommissionRate(@ToolParam(description = "交易所代码，例如 SHFE、DCE、CZCE、CFFEX") String exchange) {
		return switch (exchange.toUpperCase()) {
			case "SHFE" -> 3.0;
			case "DCE" -> 1.5;
			case "CZCE" -> 2.0;
			case "CFFEX" -> 0.23;
			default -> -1;
		};
	}

	/**
	 * 查询交易所的中文全称
	 * @param exchange 交易所代码，如 SHFE、DCE、CZCE、CFFEX
	 * @return 交易所中文全称
	 */
	@Tool(description = "查询指定期货交易所的中文全称")
	public String getExchangeName(@ToolParam(description = "交易所代码，例如 SHFE、DCE、CZCE、CFFEX") String exchange) {
		return switch (exchange.toUpperCase()) {
			case "SHFE" -> "上海期货交易所";
			case "DCE" -> "大连商品交易所";
			case "CZCE" -> "郑州商品交易所";
			case "CFFEX" -> "中国金融期货交易所";
			default -> "未知交易所";
		};
	}
}
