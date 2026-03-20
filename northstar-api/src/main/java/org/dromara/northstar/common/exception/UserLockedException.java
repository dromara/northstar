package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class UserLockedException extends LoginException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8355909793617316156L;

	public UserLockedException() {
		super(MessageSourceHolder.getMessageSource().getMessage("error.user_locked", null, LocaleContextHolder.getLocale()));
	}

}
