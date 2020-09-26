package tech.xuanwu.northstar.exception;

/**
 * 账户操作异常
 * @author kevinhuangwl
 *
 */
public class AccountException extends Exception{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2871844317264575746L;

	public AccountException() {}
	
	public AccountException(String accountName, String message) {
		super("账户【" + accountName + "】操作异常。原因：" + message);
	}
}
