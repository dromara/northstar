package tech.xuanwu.northstar.gateway.api;

import xyz.redtorch.pb.CoreField.GatewaySettingField;

public interface Gateway {

	/**
	 * 获取网关配置
	 * 
	 * @return
	 */
	GatewaySettingField getGatewaySetting();
	
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
