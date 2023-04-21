package org.dromara.northstar.common.constant;

import lombok.Getter;

/**
 * 平仓策略
 * @author KevinHuangwl
 *
 */
public enum ClosingPolicy {

	/**
	 * 按开仓的时间顺序平仓
	 */
	FIRST_IN_FIRST_OUT("先开先平"),
	/**
	 * 按开仓的时间倒序平仓
	 */
	FIRST_IN_LAST_OUT("平今优先"),
	/**
	 * 优先平掉历史持仓，对冲锁仓今天的持仓
	 */
	CLOSE_NONTODAY_HEGDE_TODAY("平昨锁今");

	@Getter
	private String name;
	private ClosingPolicy(String name) {
		this.name = name;
	}
	
}
