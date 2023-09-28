package org.dromara.northstar.strategy;

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
	 * 设置接收人
	 * @param receiver
	 */
	void addReceiver(String receiver);
}
