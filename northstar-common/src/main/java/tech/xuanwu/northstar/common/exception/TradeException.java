package tech.xuanwu.northstar.common.exception;

/**
 * 交易异常
 * @author KevinHuangwl
 *
 */
public class TradeException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3362623046560159440L;

	public TradeException() {
		super();
	}

	public TradeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public TradeException(String message, Throwable cause) {
		super(message, cause);
	}

	public TradeException(String message) {
		super(message);
	}

	public TradeException(Throwable cause) {
		super(cause);
	}

	
}
