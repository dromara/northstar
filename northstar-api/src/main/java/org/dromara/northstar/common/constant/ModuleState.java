package org.dromara.northstar.common.constant;

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
	 * 持多仓
	 */
	HOLDING_LONG,
	/**
	 * 持空仓
	 */
	HOLDING_SHORT,
	/**
	 * 持仓对冲锁仓（合约相同，持仓数量相等，方向相反）
	 */
	EMPTY_HEDGE,
	/**
	 * 套利持仓（合约不同，持仓数量相等，方向相反）
	 */
	HOLDING_HEDGE,
	/**
	 * 下单中
	 */
	PLACING_ORDER,
	/**
	 * 等待订单反馈
	 */
	PENDING_ORDER,
	/**
	 * 撤单
	 */
	RETRIEVING_FOR_CANCEL;
	
	
	public boolean isHolding() {
		return this == HOLDING_LONG || this == HOLDING_SHORT || this == HOLDING_HEDGE;
	}
	
	public boolean isWaiting() {
		return this == PENDING_ORDER || this == RETRIEVING_FOR_CANCEL;
	}
	
	public boolean isOrdering() {
		return this == PLACING_ORDER || isWaiting();
	}
	
	public boolean isEmpty() {
		return this ==  EMPTY || this == EMPTY_HEDGE;
	}
}
