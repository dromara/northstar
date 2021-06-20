package tech.xuanwu.northstar.strategy.common.constants;

/**
 * 策略模组状态
 * @author KevinHuangwl
 *
 */
public enum ModuleState {

	/**
	 * 空仓
	 */
	EMPTY,
	/**
	 * 持仓
	 */
	HOLDING,
	/**
	 * 下单中
	 */
	PLACING_ORDER,
	/**
	 * 追单中
	 */
	TRACING_ORDER;
}
