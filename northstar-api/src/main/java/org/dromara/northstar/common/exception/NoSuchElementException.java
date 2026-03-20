package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 找不到相应元素异常
 * @author KevinHuangwl
 *
 */
public class NoSuchElementException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6631205607185324276L;

	public NoSuchElementException(String elType, String item) {
		super(MessageSourceHolder.getMessageSource().getMessage("error.no_such_element", new Object[] { elType, item }, LocaleContextHolder.getLocale()));
	}
}
