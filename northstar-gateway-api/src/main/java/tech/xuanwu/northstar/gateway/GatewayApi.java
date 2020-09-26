package tech.xuanwu.northstar.gateway;

import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewayField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public interface GatewayApi {

	/**
	 * 获取网关
	 * 
	 * @return
	 */
	GatewayField getGateway();
	
	/**
	 * 获取网关配置
	 * 
	 * @return
	 */
	GatewaySettingField getGatewaySetting();
	
	/**
	 * 订阅
	 * 
	 * @param subscribeReq
	 */
	boolean subscribe(ContractField contract);

	/**
	 * 退订
	 * 
	 * @param subscribeReq
	 */
	boolean unsubscribe(ContractField contract);

	/**
	 * 提交定单
	 * 
	 * @param orderReq
	 * @return
	 */
	String submitOrder(SubmitOrderReqField submitOrderReq);

	/**
	 * 撤销定单
	 * 
	 * @param cancelOrderReq
	 * @return
	 */
	boolean cancelOrder(CancelOrderReqField cancelOrderReq);
	
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

	/**
	 * 获取最后一次开始登陆的时间戳
	 * 
	 * @return
	 */
	long getLastConnectBeginTimestamp();

}
