package tech.xuanwu.northstar.strategy.common.event;

public enum ModuleEventType {
	/**
	 * 信号生成
	 */
	SIGNAL_CREATED,
	/**
	 * 订单请求生成
	 */
	ORDER_REQ_CREATED,
	/**
	 * 订单请求通过风控审核
	 */
	ORDER_REQ_ACCEPTED,
	/**
	 * 风控指引
	 */
	ORDER_RETRY;
}
