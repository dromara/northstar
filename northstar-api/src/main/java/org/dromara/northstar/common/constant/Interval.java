package org.dromara.northstar.common.constant;

public enum Interval {

	M1(1, "1m"),
	
	M3(3, "3m"),
	
	M5(5, "5m"),
	
	M10(10, "10m"),
	
	M15(15, "15m"),
	
	M30(30, "30m"),
	
	H1(60, "1h"),
	
	H2(120, "2h"),
	
	H4(240, "4h"),
	
	D1(1440, "1d"),
	
	WK1(10080, "1wk");
	
	private int minutes;
	private String alias;
	private Interval(int minutes, String alias) {
		this.minutes = minutes;
		this.alias = alias;
	}
	
	public int getMinutes() {
		return minutes;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public static Interval fromAlias(String alias) {
		for(Interval interval : values()) {
			if(interval.alias.equals(alias)) {
				return interval;
			}
		}
		throw new IllegalArgumentException("不支持的K线周期: " + alias);
	}
}
