package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class NoSuchModuleException extends NoSuchElementException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8723732085278143228L;

	public NoSuchModuleException(String item) {
		super(MessageSourceHolder.getMessageSource().getMessage("element.module", null, LocaleContextHolder.getLocale()), item);
	}

}
