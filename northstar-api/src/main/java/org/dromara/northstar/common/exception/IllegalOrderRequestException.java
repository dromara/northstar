package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class IllegalOrderRequestException extends TradeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7705713631152358032L;

	public IllegalOrderRequestException() {
		super(MessageSourceHolder.getMessageSource().getMessage("error.illegal_order", null, LocaleContextHolder.getLocale()));
	}
}
