package tech.xuanwu.northstar.trader.domain.simulated.exception;

public class UnsufficientBalanceException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4055084330416620750L;

	public UnsufficientBalanceException() {}
	
	public UnsufficientBalanceException(String message) {
		super(message);
	}
	
}
