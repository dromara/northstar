package tech.xuanwu.northstar.strategy.common.event;

public enum ModuleEventType {
	/**
	 * 信号生成
	 */
	SIGNAL_CREATED,
	/**
	 * 信号受限（风控限制）
	 */
	SIGNAL_RETAINED,
	/**
	 * 订单提交
	 */
	ORDER_SUBMITTED,
	/**
	 * 挂单成交
	 */
	ORDER_TRADED,
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
