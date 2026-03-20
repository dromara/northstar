package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class OperationException extends IllegalStateException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -9129148014838270603L;

	public OperationException(String msgCode, Object...params) {
		super(MessageSourceHolder.getMessageSource().getMessage(msgCode, params, LocaleContextHolder.getLocale()));
	}

}
