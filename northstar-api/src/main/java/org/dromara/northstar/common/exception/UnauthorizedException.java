package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class UnauthorizedException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9021919062201500258L;
	
	private String resourceName;

	public UnauthorizedException(String resourceName) {
		super(resourceName);
		this.resourceName = resourceName;
	}
	
	@Override
	public String getLocalizedMessage() {
		return MessageSourceHolder.getMessageSource().getMessage("error.unauthorized", new Object[] { resourceName }, LocaleContextHolder.getLocale());
	}

	@Override
	public String getMessage() {
		return getLocalizedMessage();
	}
	
}
