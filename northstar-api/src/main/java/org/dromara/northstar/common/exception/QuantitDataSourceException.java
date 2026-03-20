package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class QuantitDataSourceException extends RuntimeException{

	private static final long serialVersionUID = 1L;
	
	public QuantitDataSourceException(Throwable cause) {
		super(cause);
	}

	@Override
	public String getLocalizedMessage() {
		return MessageSourceHolder.getMessageSource().getMessage("error.datasource", null, LocaleContextHolder.getLocale());
	}
}
