package org.dromara.northstar.indicator.constant;

/**
 * 周期单位
 * @author KevinHuangwl
 *
 */
public enum PeriodUnit{
	/**
	 * 分钟
	 */
	MINUTE("m"),
	/**
	 * 小时
	 */
	HOUR("hr"),
	/**
	 * 天
	 */
	DAY("d");
	
	String symbol;
	private PeriodUnit(String unitSymbol) {
		symbol = unitSymbol;
	}
	
	public String symbol() {
		return symbol;
	}
}