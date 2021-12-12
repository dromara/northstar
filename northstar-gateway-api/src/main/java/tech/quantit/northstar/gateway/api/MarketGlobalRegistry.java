package tech.quantit.northstar.gateway.api;

import java.util.EnumMap;
import java.util.Map;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.event.FastEventEngine;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * CTP网关全局注册中心，负责路由行情数据到合适的BAR生成器
 * @author KevinHuangwl
 *
 */
public class MarketGlobalRegistry {

	private FastEventEngine feEngine;
	
	private Map<GatewayType, SubscriptionManager> csmMap;
	
	public MarketGlobalRegistry(FastEventEngine feEngine, Map<GatewayType, SubscriptionManager> csmMap) {
		this.feEngine = feEngine;
		this.csmMap = csmMap;
	}
	
	public void register(IContract contract) {
		
	}
	
	public void routeToBarGenerator(TickField tick) {
		
	}
}
