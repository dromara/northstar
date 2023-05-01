package org.dromara.northstar.indicator;

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
	DAY("d"),
	/**
	 * 周
	 */
	WEEK("wk"),
	/**
	 * 月
	 */
	MONTH("M");
	
	String symbol;
	private PeriodUnit(String unitSymbol) {
		symbol = unitSymbol;
	}
	
	public String symbol() {
		return symbol;
	}
}