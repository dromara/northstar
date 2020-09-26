package tech.xuanwu.northstar.trader.domain.simulated.exception;

import tech.xuanwu.northstar.exception.NorthstarException;

public class UnsupportedMethodException extends NorthstarException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1353299273833863375L;

	public UnsupportedMethodException() {}
	
	public UnsupportedMethodException(String message) {
		super(message);
	}
}
