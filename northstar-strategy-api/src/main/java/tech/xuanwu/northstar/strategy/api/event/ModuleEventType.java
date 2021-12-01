package tech.xuanwu.northstar.strategy.api.event;

public enum ModuleEventType {
	/**
	 * 信号生成
	 */
	SIGNAL_CREATED,
	/**
	 * 订单生成
	 */
	ORDER_REQ_CREATED,
	/**
	 * 订单受限（风控限制）
	 */
	ORDER_REQ_RETAINED,
	/**
	 * 订单通过审核
	 */
	ORDER_REQ_ACCEPTED,
	/**
	 * 撤单
	 */
	ORDER_REQ_CANCELLED,
	/**
	 * 订单确认
	 */
	ORDER_CONFIRMED,
	/**
	 * 多单成交
	 */
	BUY_TRADED,
	/**
	 * 空单成交
	 */
	SELL_TRADED,
	/**
	 * 止损
	 */
	STOP_LOSS,
	/**
	 * 风控重试
	 */
	RETRY_RISK_ALERTED,
	/**
	 * 风控拒绝
	 */
	REJECT_RISK_ALERTED,
	/**
	 * 挂单撤销
	 */
	ORDER_CANCELLED;
}
