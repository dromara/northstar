package tech.xuanwu.northstar.strategy.common.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface Setting {

	String value() default "";
	
	String unit() default "";
	
	int order() default 0;
	
	String[] options() default {};
}
