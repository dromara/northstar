package tech.xuanwu.northstar.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.domain.MarketGatewayConnection;
import tech.xuanwu.northstar.domain.TraderGatewayConnection;
import tech.xuanwu.northstar.gateway.api.Gateway;

@Slf4j
@Service
public class GatewayService implements InitializingBean {
	
	private ConcurrentHashMap<String, MarketGatewayConnection> marketGatewayMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, TraderGatewayConnection> traderGatewayMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<GatewayConnection, Gateway> gatewayMap = new ConcurrentHashMap<>();
	
	/**
	 * 创建网关
	 * @return
	 */
	public boolean createGateway(GatewayDescription gatewayDescription) {
		
		return true;
	}
	
	/**
	 * 更新网关
	 * @return
	 */
	public boolean updateGateway(GatewayDescription gatewayDescription) {
		
		return true;
	}
	
	/**
	 * 移除网关
	 * @return
	 */
	public boolean deleteGateway(String gatewayId) {
		
		return true;
	}
	
	/**
	 * 查询所有网关
	 * @return
	 */
	public List<GatewayDescription> findAllGateway(){
		
		return null;
	}
	
	/**
	 * 查询所有网关
	 * @return
	 */
	public List<GatewayDescription> findAllMarketGateway(){
		
		return null;
	}
	
	/**
	 * 查询所有网关
	 * @return
	 */
	public List<GatewayDescription> findAllTraderGateway(){
		
		return null;
	}
	
	/**
	 * 连接网关
	 * @return
	 */
	public boolean connect(String gatewayId) {
		return true;
	}
	
	/**
	 * 断开网关
	 * @return
	 */
	public boolean disconnect(String gatewayId) {
		
		return true;
	}
	
	/**
	 * 下单
	 * @return
	 */
	public String submitOrder(OrderRequest req) {
		return "";
	}
	
	/**
	 * 撤单
	 * @return
	 */
	public boolean cancelOrder() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		
	}
}
