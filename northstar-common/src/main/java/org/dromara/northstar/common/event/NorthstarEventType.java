package org.dromara.northstar.common.event;

/**
 * 系统事件列表
 * @author KevinHuangwl
 *
 */
public enum NorthstarEventType {
	/**
	 * 行情TICK事件
	 */
	TICK,
	/**
	 * K线BAR事件
	 */
	BAR,
	/**
	 * 账户回报事件
	 */
	ACCOUNT,
	/**
	 * 持仓回报事件
	 */
	POSITION,
	/**
	 * 成交回报事件
	 */
	TRADE,
	/**
	 * 委托回报事件
	 */
	ORDER,
	/**
	 * 消息事件
	 */
	NOTICE,
	/**
	 * 合约事件
	 */
	CONTRACT,
	/**
	 * 出入金事件
	 */
	BALANCE,
	/**
	 * 登陆中
	 */
	LOGGING_IN,
	/**
	 * 登陆成功
	 */
	LOGGED_IN,
	/**
	 * 登出中
	 */
	LOGGING_OUT,
	/**
	 * 登出成功
	 */
	LOGGED_OUT,
	/**
	 * 网关就绪
	 */
	GATEWAY_READY,
	
}
