package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class LoginException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5940992686393711608L;

	public LoginException(String reason) {
		super(MessageSourceHolder.getMessageSource().getMessage("error.login_error", new Object[] {reason}, LocaleContextHolder.getLocale()));
	}
}
