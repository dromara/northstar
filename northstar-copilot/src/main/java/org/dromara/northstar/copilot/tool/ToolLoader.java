package org.dromara.northstar.copilot.tool;

import java.lang.reflect.Modifier;
import java.util.List;

import org.reflections.Reflections;
import org.springframework.util.Assert;

/**
 * 工具动态加载器：运行时扫描指定包下所有标注 {@link CopilotTool} 的类并实例化，
 * 返回的工具对象可直接交给 Spring AI 的 {@code tools(...)} 方法注册为大模型工具。
 * <p>
 * 使用示例（对所有 OpenAI 兼容平台通用，包括 Kimi）：
 * <pre>{@code
 * String content = chatClient.prompt()
 *         .user("上期所每手手续费是多少？")
 *         .tools(ToolLoader.load("org.dromara.northstar.copilot"))  // 动态加载工具
 *         .call()
 *         .content();
 * }</pre>
 * 注意：自 Spring AI 2.0.0 起，{@code toolCallbacks(...)} 系列方法已废弃，
 * 统一使用 {@code tools(Object...)} 注册工具对象，Spring AI 会自动提取其中的
 * {@code @Tool} 注解方法。
 * <p>
 * 工具集合是在每次请求时解析的，因此不同请求可以加载不同的工具组合；
 * 如需对某个 ChatClient 全局生效，可在构建时调用
 * {@code ChatClient.builder(chatModel).defaultTools(...)}。
 */
public final class ToolLoader {

	private ToolLoader() {}

	/**
	 * 扫描并加载指定包下的全部工具
	 * @param basePackage 要扫描的根包名
	 * @return 扫描到的工具实例列表，可用于 {@code prompt().tools(...)}
	 */
	public static List<Object> load(String basePackage) {
		Assert.hasText(basePackage, "basePackage 不能为空");
		return new Reflections(basePackage)
				.getTypesAnnotatedWith(CopilotTool.class).stream()
				.filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
				.map(ToolLoader::instantiate)
				.toList();
	}

	private static Object instantiate(Class<?> clazz) {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("工具类实例化失败（需提供无参构造器）: " + clazz.getName(), e);
		}
	}
}
