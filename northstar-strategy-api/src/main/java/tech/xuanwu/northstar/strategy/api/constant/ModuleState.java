package tech.xuanwu.northstar.strategy.api.constant;

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
	 * 净空仓（锁仓）
	 */
	NET_EMPTY,
	/**
	 * 持多仓
	 */
	HOLDING_LONG,
	/**
	 * 持空仓
	 */
	HOLDING_SHORT,
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
	RETRIEVING_FOR_CANCAL,
	/**
	 * 撤单重试
	 */
	RETRIEVING_FOR_RETRY;
	
	
	public boolean isHolding() {
		return this == HOLDING_LONG || this == HOLDING_SHORT;
	}
	
	public boolean isWaiting() {
		return this == PENDING_ORDER || this == RETRIEVING_FOR_CANCAL || this == RETRIEVING_FOR_RETRY;
	}
	
	public boolean isOrdering() {
		return this == PLACING_ORDER || isWaiting();
	}
	
	public boolean isEmpty() {
		return this ==  EMPTY || this == NET_EMPTY;
	}
}
