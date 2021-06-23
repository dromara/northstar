package tech.xuanwu.northstar.strategy.common.constants;

/**
 * 使用四位二进制数表达六个状态
 * 	0	0	| 	0	0
 * 四位二进制可以看作是两个两位二进制的组合
 * 其中左边位，1代表多，0代表空；右边位，1代表开，0代表平
 * 
 * 反手本质上是两组运作的叠加，例如反手多=多平+多开
 * 因此由该二进制编码可得规律，并总结成枚举方法如下
 * 
 * @author KevinHuangwl
 *
 */
public enum SignalState {
	/**
	 * 多开
	 */
	BuyOpen(0b0011),
	/**
	 * 空开
	 */
	SellOpen(0b0001),
	/**
	 * 多平
	 */
	BuyClose(0b0010),
	/**
	 * 空平
	 */
	SellClose(0b0000),
	/**
	 * 反手多
	 */
	ReversingBuy(0b1110),
	/**
	 * 反手空
	 */
	ReversingSell(0b0100);

	
	private int val;
	private SignalState(int val) {
		this.val = val;
	}
	
	public boolean isOpen() {
		return (val & 5) > 0;
	}
	
	public boolean isReverse() {
		return val > 3;
	}
	
	public boolean isBuy() {
		return (val & 2) > 0;
	}
}
