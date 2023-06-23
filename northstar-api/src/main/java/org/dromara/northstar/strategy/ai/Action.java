package org.dromara.northstar.strategy.ai;

public enum Action {
	/**
	 * 做多
	 */
	BUY(1),
	/**
	 * 做空
	 */
	SELL(-1),
	/**
	 * 什么都不做
	 */
	NONE(0);

	private Action(int i) {}
	
	public static Action parse(int i) {
		if(i == 1)	return BUY;
		if(i == -1)	return SELL;
		if(i == 0) return NONE;
		throw new IllegalArgumentException("未定义Action：" + i);
	}
}
