package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class IPFrozenException extends LoginException {

	private static final long serialVersionUID = 1L;

	public IPFrozenException(long remainingSeconds) {
		super(MessageSourceHolder.getMessageSource().getMessage("error.ip_frozen", new Object[] { remainingSeconds / 60 }, LocaleContextHolder.getLocale()));
	}

}
