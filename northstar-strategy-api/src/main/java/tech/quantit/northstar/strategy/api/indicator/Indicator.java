package tech.quantit.northstar.strategy.api.indicator;

import tech.quantit.northstar.strategy.api.BarDataAware;

/**
 * 行情指标抽象类
 * @author KevinHuangwl
 *
 */
public abstract class Indicator implements BarDataAware {
	

	
	/**
	 * 指标取值类型
	 * @author KevinHuangwl
	 *
	 */
	public static enum ValueType {
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
		 * 成交量
		 */
		VOL,
		/**
		 * 持仓量
		 */
		OPEN_INTEREST;
	}
}
