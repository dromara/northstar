package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class NoSuchGatewayException extends NoSuchElementException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2889095565514035846L;

	public NoSuchGatewayException(String item) {
		super(MessageSourceHolder.getMessageSource().getMessage("element.gateway", null, LocaleContextHolder.getLocale()), item);
	}

}
