package org.dromara.northstar.copilot.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author cly
 * @Description TODO
 * @Date 2024/8/12 15:55
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FunctionParameter {
    String description();
    boolean required() default true;
}