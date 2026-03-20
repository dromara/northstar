package org.dromara.northstar.strategy;

import java.util.Collection;

import org.dromara.northstar.common.model.ComponentField;

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
	 * 异步发送信息
	 * @param title
	 * @param content
	 */
	void asyncSend(String title, String content);
	/**
	 * 异步发送信息
	 * @param content
	 */
	void asyncSend(String content);
	/**
	 * 更新设置
	 * @param settings
	 */
	void setMsgSenderSettings(Collection<ComponentField> settings);
}
