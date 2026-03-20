package org.dromara.northstar.common.exception;

import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 交易异常
 * @author KevinHuangwl
 *
 */
public class TradeException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3362623046560159440L;

	protected TradeException(String reason) {
		super(MessageSourceHolder.getMessageSource().getMessage("error.trade_error", new Object[]{reason}, LocaleContextHolder.getLocale()));
	}
	
	public <T extends TradeException> TradeException(T t){
		super(MessageSourceHolder.getMessageSource().getMessage("error.trade_error", new Object[]{t.getMessage()}, LocaleContextHolder.getLocale()));
	}

}
