package org.dromara.northstar.common.constant;

/**
 * 回放精度
 * @author KevinHuangwl
 *
 */
public enum PlaybackPrecision {
	/**
	 * 每分钟1TICK
	 */
	LITE,	
	/**
	 * 每分钟4TICK
	 */
	LOW,
	/**
	 * 每分钟30TICK
	 */
	MEDIUM,
	/**
	 * 每分钟120TICK
	 */
	HIGH;
}
