package org.dromara.northstar.strategy;

import org.dromara.northstar.common.event.NorthstarEvent;

public interface IMessageSender {

	/**
	 * 发送信息
	 * @param title		标题
	 * @param message	内容
	 */
	void send(String title, String content);
	/**
	 * 发送信息
	 * @param content	内容
	 */
	void send(String content);
	/**
	 * 处理系统事件
	 * @param e
	 */
	void onEvent(NorthstarEvent e);
}
