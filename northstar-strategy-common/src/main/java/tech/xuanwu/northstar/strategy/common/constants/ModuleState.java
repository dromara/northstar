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
	 * 开仓委托中
	 */
	OPENNING,
	/**
	 * 平仓委托中
	 */
	CLOSING, 
	/**
	 * 撤单中
	 */
	CANCELING_ORDER;
}
