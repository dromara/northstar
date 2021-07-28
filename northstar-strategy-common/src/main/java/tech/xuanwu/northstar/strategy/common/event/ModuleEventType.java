package tech.xuanwu.northstar.strategy.common.event;

public enum ModuleEventType {
	/**
	 * 开仓信号生成
	 */
	OPENING_SIGNAL_CREATED,
	/**
	 * 平仓信号生成
	 */
	CLOSING_SIGNAL_CREATED,
	/**
	 * 信号受限（风控限制）
	 */
	SIGNAL_RETAINED,
	/**
	 * 订单提交
	 */
	ORDER_SUBMITTED,
	/**
	 * 多单成交
	 */
	BUY_TRADED,
	/**
	 * 空单成交
	 */
	SELL_TRADED,
	/**
	 * 风控预警
	 */
	RISK_ALERTED,
	/**
	 * 挂单撤销
	 */
	ORDER_CANCELLED,
	/**
	 * 追单重试
	 */
	ORDER_RETRY,
}
