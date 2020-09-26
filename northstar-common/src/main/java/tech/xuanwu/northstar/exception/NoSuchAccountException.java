package tech.xuanwu.northstar.exception;

public class NoSuchAccountException extends NorthstarException{
	private static final long serialVersionUID = 3657194520588995026L;

	public NoSuchAccountException(){}
	
	public NoSuchAccountException(String accountName) {
		super("不存在ID为【" + accountName + "】的账户");
	}
	
}
