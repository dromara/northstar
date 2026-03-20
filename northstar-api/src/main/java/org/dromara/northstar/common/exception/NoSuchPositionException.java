package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class NoSuchPositionException extends NoSuchElementException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7909410719266412914L;

	public NoSuchPositionException(String item) {
		super(MessageSourceHolder.getMessageSource().getMessage("element.position", null, LocaleContextHolder.getLocale()), item);
	}
	
}
