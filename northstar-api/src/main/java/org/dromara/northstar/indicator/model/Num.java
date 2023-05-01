package org.dromara.northstar.indicator.model;

/**
 * 值对象
 * @param 		数据值
 * @param		值对应的时间戳
 * @param		不稳定标识
 * @author KevinHuangwl
 *
 */
public record Num(double value, long timestamp, boolean unstable) {

	public static Num of(double value, long timestamp) {
		return new Num(value, timestamp, false);
	}
	
	public static Num of(double value, long timestamp, boolean unstable) {
		return new Num(value, timestamp, unstable);
	}
	
	public static Num NaN() {
		return Num.of(Double.NaN, 0, true);
	}
	
	public boolean isNaN() {
		return Double.isNaN(value);
	}
}
