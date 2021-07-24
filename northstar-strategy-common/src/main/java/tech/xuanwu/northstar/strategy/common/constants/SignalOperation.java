package tech.xuanwu.northstar.strategy.common.constants;

/**
 * 使用四位二进制数表达六个状态
 * 
 * 反手本质上是两组运作的叠加，例如反手多=多平+多开
 * 因此由该二进制编码可得规律，并总结成枚举方法如下
 * 
 * @author KevinHuangwl
 *
 */
public enum SignalOperation {
	/**
	 * 多开
	 */
	BuyOpen(0b0001),
	/**
	 * 空开
	 */
	SellOpen(0b0010),
	/**
	 * 多平
	 */
	BuyClose(0b0100),
	/**
	 * 空平
	 */
	SellClose(0b1000),
	/**
	 * 反手多
	 */
	ReversingBuy(0b0101),
	/**
	 * 反手空
	 */
	ReversingSell(0b1010),
	/**
	 * 无信号
	 */
	None(0b0000);

	
	private int val;
	private SignalOperation(int val) {
		this.val = val;
	}
	
	public boolean isOpen() {
		return (val & 3) > 0;
	}
	
	public boolean isReverse() {
		return val == 5 || val == 10;
	}
	
	public boolean isBuy() {
		return (val & 0b0101) > 0;
	}
	
	public int code() {
		return val;
	}
}
