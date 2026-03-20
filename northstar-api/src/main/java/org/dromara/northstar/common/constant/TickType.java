package org.dromara.northstar.common.constant;

public enum TickType {
	/**
	 * 回测预热行情
	 */
	PLAYBACK_DUMMY_TICK,
	/**
	 * 回测行情
	 */
	PLAYBACK_TICK,
	/**
	 * 非交易时间的信息行情
	 */
	INFO_TICK,
	/**
	 * 实时行情
	 */
	MARKET_TICK;
	
}
