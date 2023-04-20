package org.dromara.northstar.common.exception;

/**
 * 数量不足异常
 * @author KevinHuangwl
 *
 */
public class InsufficientException extends TradeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7387299700651706659L;

	public InsufficientException() {
		super();
	}

	public InsufficientException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InsufficientException(String message, Throwable cause) {
		super(message, cause);
	}

	public InsufficientException(String message) {
		super(message);
	}

	public InsufficientException(Throwable cause) {
		super(cause);
	}
	
}
