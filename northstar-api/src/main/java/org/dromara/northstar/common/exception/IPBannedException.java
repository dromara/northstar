package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class IPBannedException extends LoginException {

	private static final long serialVersionUID = 1L;

	public IPBannedException(long remainingSeconds) {
		super(MessageSourceHolder.getMessageSource().getMessage("error.ip_banned", new Object[] { remainingSeconds / 3600 }, LocaleContextHolder.getLocale()));
	}

}
