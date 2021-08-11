package tech.xuanwu.northstar.strategy.common.model;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;

@Builder
@Data
public class CtaSignal implements Signal{
	
	/**
	 * 信号ID
	 */
	private UUID id;
	/**
	 * 信号状态
	 */
	private SignalOperation state;
	/**
	 * 信号价格
	 */
	private double signalPrice;
	/**
	 * 止损价
	 */
	private double stopPrice;
	/**
	 * 信号产生时间
	 */
	private long timestamp;
	/**
	 * 关联信号策略
	 */
	private Class<? extends SignalPolicy> signalClass;
	/**
	 * 信号源合约
	 */
	private String sourceUnifiedSymbol;
	
	
	@Override
	public boolean isOpening() {
		return state.isOpen();
	}
	
}
