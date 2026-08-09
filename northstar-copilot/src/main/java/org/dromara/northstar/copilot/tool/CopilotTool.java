package org.dromara.northstar.copilot.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Copilot 工具类的标记注解。
 * <p>
 * 在任意包含 {@code @Tool} 方法的类上添加本注解，即可被 {@link ToolLoader}
 * 在运行时自动扫描发现并注册为大模型工具，无需在使用处手动 {@code new} 出来。
 * <p>
 * 注意：被标记的工具类必须提供无参构造器。
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CopilotTool {
}
