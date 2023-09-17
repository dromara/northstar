package org.dromara.northstar.common;

import org.dromara.northstar.common.model.GatewayDescription;

public interface IGatewayService {

	/**
	 * 创建网关
	 * @return
	 * @throws Exception 
	 */
	boolean createGateway(GatewayDescription gatewayDescription);

	/**
	 * 更新网关
	 * @return
	 * @throws Exception 
	 */
	boolean updateGateway(GatewayDescription gatewayDescription);

	/**
	 * 移除网关
	 * @return
	 */
	boolean deleteGateway(String gatewayId);

	/**
	 * 连接网关
	 * @return
	 */
	boolean connect(String gatewayId);

	/**
	 * 断开网关
	 * @return
	 */
	boolean disconnect(String gatewayId);

	/**
	 * 模拟出入金
	 * @param money
	 * @return
	 */
	boolean simMoneyIO(String gatewayId, int money);
	
	/**
	 * 复位重置回放网关
	 * @param gatewayId
	 * @return
	 * @throws Exception 
	 */
	boolean resetPlayback(String gatewayId);

}
