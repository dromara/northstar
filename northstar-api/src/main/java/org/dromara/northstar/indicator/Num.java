package org.dromara.northstar.indicator;

/**
 * 值对象
 * @param 		数据值
 * @param		不稳定标识
 * @author KevinHuangwl
 *
 */
public record Num(double value, boolean unstable) {

	public static Num of(double value) {
		return new Num(value, false);
	}
	
	public static Num of(double value, boolean unstable) {
		return new Num(value, unstable);
	}
	
	public boolean isNaN() {
		return Double.isNaN(value);
	}
}
