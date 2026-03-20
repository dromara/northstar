package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

public class NoSuchContractException extends NoSuchElementException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3275031709648649946L;

	public NoSuchContractException(String item) {
		super(MessageSourceHolder.getMessageSource().getMessage("element.contract", null, LocaleContextHolder.getLocale()), item);
	}

}
