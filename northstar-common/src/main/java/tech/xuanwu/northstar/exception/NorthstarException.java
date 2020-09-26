package tech.xuanwu.northstar.exception;

/**
 * 系统通用异常
 * @author kevinhuangwl
 *
 */
public class NorthstarException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3366733550261336878L;

	public NorthstarException() {}
	
	public NorthstarException(String message) {
		super(message);
	}
	
	public NorthstarException(String message, Throwable t) {
		super(message, t);
	}
}
