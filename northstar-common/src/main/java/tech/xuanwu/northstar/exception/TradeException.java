package tech.xuanwu.northstar.exception;

public class TradeException extends NorthstarException{

	private static final long serialVersionUID = 4927181190481647085L;

	public TradeException(){}
	
	public TradeException(String accountName, String message) {
		super("账户【" + accountName + "】交易异常。原因：" + message);
	}
}
