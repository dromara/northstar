package org.dromara.northstar.common.exception;

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

	public NoSuchElementException() {
		super();
	}

	public NoSuchElementException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NoSuchElementException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoSuchElementException(String message) {
		super(message);
	}

	public NoSuchElementException(Throwable cause) {
		super(cause);
	}

}
