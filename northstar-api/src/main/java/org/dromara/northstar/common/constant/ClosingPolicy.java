package org.dromara.northstar.common.constant;

/**
 * 平仓策略
 * @author KevinHuangwl
 *
 */
public enum ClosingPolicy {

	/**
	 * 按开仓的时间顺序平仓
	 */
	FIRST_IN_FIRST_OUT,
	/**
	 * 按开仓的时间倒序平仓
	 */
	FIRST_IN_LAST_OUT,
	/**
	 * 优先平掉历史持仓，对冲锁仓今天的持仓
	 * 注意：这里只有锁仓逻辑，没有解锁逻辑
	 */
	CLOSE_NONTODAY_HEGDE_TODAY;

}
