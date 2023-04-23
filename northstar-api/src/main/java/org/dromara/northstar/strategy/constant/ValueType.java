package org.dromara.northstar.strategy.constant;

/**
 * 指标取值类型
 * @author KevinHuangwl
 *
 */
public enum ValueType {
	/**
	 * 未设置
	 */
	NOT_SET,
	/**
	 * 最高价
	 */
	HIGH,
	/**
	 * 最低价
	 */
	LOW,
	/**
	 * 开盘价
	 */
	OPEN,
	/**
	 * 收盘价
	 */
	CLOSE,
	/**
	 * 重心价
	 */
	BARYCENTER,
	/**
	 * 成交量
	 */
	VOL,
	/**
	 * 持仓量
	 */
	OI,
	/**
	 * 持仓量变化
	 */
	OI_DELTA;
}