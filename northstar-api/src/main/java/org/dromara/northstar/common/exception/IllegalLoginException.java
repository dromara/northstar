package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class IllegalLoginException extends LoginException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -249039119010879687L;

	public IllegalLoginException() {
		super(MessageSourceHolder.getMessageSource().getMessage("error.invalid_info", null, LocaleContextHolder.getLocale()));
	}

}
