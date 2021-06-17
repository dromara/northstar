package tech.xuanwu.northstar.strategy.common.constants;

public enum SignalState {
	/**
	 * 多开
	 */
	BuyOpen,
	/**
	 * 空开
	 */
	SellOpen,
	/**
	 * 多平
	 */
	BuyClose,
	/**
	 * 空平
	 */
	SellClose,
	/**
	 * 反手多
	 */
	ReversingBuy,
	/**
	 * 反手空
	 */
	ReversingSell;

}
