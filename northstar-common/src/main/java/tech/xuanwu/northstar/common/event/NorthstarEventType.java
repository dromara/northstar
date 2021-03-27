package tech.xuanwu.northstar.common.event;

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
	 * 账户事件
	 */
	ACCOUNT,
	/**
	 * 持仓事件
	 */
	POSITION,
	/**
	 * 成交事件
	 */
	TRADE,
	/**
	 * 委托事件
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
	 * 连线中
	 */
	CONNECTING,
	/**
	 * 连线成功
	 */
	CONNECTED,
	/**
	 * 登陆中
	 */
	LOGINING,
	/**
	 * 登陆成功
	 */
	LOGINED,
	/**
	 * 断开中
	 */
	DISCONNECTING,
	/**
	 * 断开成功
	 */
	DISCONNECTED,
	/**
	 * 交易日更新事件
	 */
	TRADE_DATE
	
}
