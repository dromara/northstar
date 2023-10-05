package org.dromara.northstar.common.constant;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.ApplicationContext;

public class GlobalSpringContext {

	public static final AtomicReference<ApplicationContext> INSTANCE = new AtomicReference<>();
}
