package tech.quantit.northstar.common.constant;

public enum IndicatorType {
	UNKNOWN,
	/**
	 * 基于价格
	 */
	PRICE_BASE,
	/**
	 * 基于成交量
	 */
	VOLUME_BASE,
	/**
	 * 基于持仓量
	 */
	OPEN_INTEREST_BASE;
}
