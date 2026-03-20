package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class NoConnectionException extends TradeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8421811475264021820L;

	public NoConnectionException() {
		super(MessageSourceHolder.getMessageSource().getMessage("error.not_tradable", null, LocaleContextHolder.getLocale()));
	}

}
