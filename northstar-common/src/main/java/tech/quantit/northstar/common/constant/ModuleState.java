package tech.quantit.northstar.common.constant;

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
	HEDGE_EQUAL,
	/**
	 * 套利持仓（多远月，空近月）
	 */
	HEDGE_LONG,
	/**
	 * 套利持仓（空远月，多近月）
	 */
	HEDGE_SHORT,
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
	RETRIEVING_FOR_CANCEL,
	/**
	 * 撤单重试
	 */
	RETRIEVING_FOR_RETRY;
	
	
	public boolean isHolding() {
		return this == HOLDING_LONG || this == HOLDING_SHORT || this == HEDGE_LONG || this == HEDGE_SHORT;
	}
	
	public boolean isWaiting() {
		return this == PENDING_ORDER || this == RETRIEVING_FOR_CANCEL || this == RETRIEVING_FOR_RETRY;
	}
	
	public boolean isOrdering() {
		return this == PLACING_ORDER || isWaiting();
	}
	
	public boolean isEmpty() {
		return this ==  EMPTY || this == HEDGE_EQUAL;
	}
}
