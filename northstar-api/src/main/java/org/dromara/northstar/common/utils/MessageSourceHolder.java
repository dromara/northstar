package org.dromara.northstar.common.utils;

import org.springframework.context.MessageSource;

public class MessageSourceHolder {

	private static MessageSource instance;
	
	private MessageSourceHolder() {}
	
	public static MessageSource getMessageSource() {
		if(instance == null) {
			throw new IllegalStateException("未初始化实例");
		}
		return instance;
	}
	
	public static void setMessageSource(MessageSource source) {
		instance = source;
	}
}
