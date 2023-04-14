package org.dromara.northstar.gateway.api;

import org.dromara.northstar.common.model.GatewayDescription;

public interface Gateway {

	/**
	 * 获取网关配置
	 * 
	 * @return
	 */
	GatewayDescription gatewayDescription();
	/**
	 * 网关ID
	 * @return
	 */
	String gatewayId();
	
	/**
	 * 连接
	 */
	void connect();

	/**
	 * 断开
	 */
	void disconnect();

	/**
	 * 网关连接状态
	 * 
	 * @return
	 */
	boolean isConnected();

	/**
	 * 获取登录错误标记
	 * 
	 * @return
	 */
	boolean getAuthErrorFlag();

}
