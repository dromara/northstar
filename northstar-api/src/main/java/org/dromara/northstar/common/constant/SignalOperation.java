package org.dromara.northstar.common.constant;

/**
 *  
 * @author KevinHuangwl
 *
 */
public enum SignalOperation {
	/**
	 * 多开，发出多单委托，结果为多头持仓+1；
	 */
	BUY_OPEN("多开"),
	/**
	 * 空开，发出空单委托，结果为空头持仓+1；
	 */
	SELL_OPEN("空开"),
	/**
	 * 多平，发出多单委托，结果为空头持仓-1；
	 */
	BUY_CLOSE("多平"),
	/**
	 * 空平，发出空单委托，结果为多头持仓-1；
	 */
	SELL_CLOSE("空平"),
	/**
	 * 先多平，再多开
	 */
	BUY_REVERSE("反手多"),
	/**
	 * 先空平，再空开
	 */
	SELL_REVERSE("反手空");
	
	private String text;
	private SignalOperation(String text) {
		this.text = text;
	}
	
	public String text() {
		return text;
	}
	
	public boolean isOpen() {
		return this == BUY_OPEN || this == SELL_OPEN;
	}
	
	public boolean isSell() {
		return this == SELL_OPEN || this == SELL_CLOSE || this == SELL_REVERSE;
	}
	
	public boolean isBuy() {
		return this == BUY_OPEN || this == BUY_CLOSE || this == BUY_REVERSE;
	}
	
	public boolean isClose() {
		return this == BUY_CLOSE || this == SELL_CLOSE;
	}
	
}
