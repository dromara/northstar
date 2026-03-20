package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class TooManyLoginException extends LoginException {

	private static final long serialVersionUID = 1L;

	public TooManyLoginException() {
		super(MessageSourceHolder.getMessageSource().getMessage("error.too_many_login", null, LocaleContextHolder.getLocale()));
	}

}
