package tech.xuanwu.northstar.trader.domain.simulated.exception;

public class UnsufficientVolumeException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9117036267731435860L;

	public UnsufficientVolumeException() {}
	
	public UnsufficientVolumeException(String message) {
		super(message);
	}
}
