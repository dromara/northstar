package tech.xuanwu.northstar.common.exception;

/**
 * 数量不匹配异常
 * @author KevinHuangwl
 *
 */
public class ValueMismatchException extends TradeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6704498774997165335L;

	public ValueMismatchException() {
		super();
	}

	public ValueMismatchException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ValueMismatchException(String message, Throwable cause) {
		super(message, cause);
	}

	public ValueMismatchException(String message) {
		super(message);
	}

	public ValueMismatchException(Throwable cause) {
		super(cause);
	}

	
}
