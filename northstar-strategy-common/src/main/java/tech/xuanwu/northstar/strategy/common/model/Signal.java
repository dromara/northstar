package tech.xuanwu.northstar.strategy.common.model;

import lombok.Data;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.constants.SignalState;

@Data
public class Signal {
	
	/**
	 * 信号所关联的合约
	 */
	private String bindedUnifiedSymbol;
	/**
	 * 信号状态
	 */
	private SignalState state;
	/**
	 * 信号价格
	 */
	private double signalPrice;
	/**
	 * 当前价格
	 */
	private double currentPrice;
	/**
	 * 信号产生时间
	 */
	private long timestamp;
	/**
	 * 信号所属交易日
	 */
	private String tradingDay;
	/**
	 * 关联信号策略
	 */
	private Class<? extends SignalPolicy> signalClass;
	
}
