package tech.xuanwu.northstar.exception;

public class NoSuchContractException extends NorthstarException{
	
	private static final long serialVersionUID = -773008190411214663L;

	
	public NoSuchContractException(){}
	
	public NoSuchContractException(String contract) {
		super("不存在名称为【" + contract + "】的合约");
	}
}
