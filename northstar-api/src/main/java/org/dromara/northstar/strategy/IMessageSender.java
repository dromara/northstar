package org.dromara.northstar.strategy;

public interface IMessageSender {

	/**
	 * 发送信息
	 * @param receiver	接收人
	 * @param title		标题
	 * @param message	内容
	 */
	void send(String receiver, String title, String content);
	/**
	 * 发送信息
	 * @param receiver	接收人
	 * @param content	内容
	 */
	void send(String receiver, String content);
}
