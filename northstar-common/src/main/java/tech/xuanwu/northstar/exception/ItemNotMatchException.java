package tech.xuanwu.northstar.exception;

public class ItemNotMatchException extends NorthstarException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5148898793995759706L;

	public ItemNotMatchException(String expect, String actual) {
		super("对象不匹配。期望【" + expect + "】，实际【" + actual + "】");
	}

}
