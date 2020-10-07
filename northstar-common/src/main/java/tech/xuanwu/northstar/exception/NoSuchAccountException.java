package tech.xuanwu.northstar.exception;

public class NoSuchAccountException extends NorthstarException{
	private static final long serialVersionUID = 3657194520588995026L;

	public NoSuchAccountException(){}
	
	public NoSuchAccountException(String gatewayId) {
		super("不存在网关ID为【" + gatewayId + "】的账户");
	}
	
}
