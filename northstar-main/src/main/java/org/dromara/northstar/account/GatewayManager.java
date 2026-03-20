package org.dromara.northstar.account;

import java.util.List;

import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.springframework.stereotype.Component;

/**
 * 网关管理器
 * @author KevinHuangwl
 *
 */
@Component
public class GatewayManager extends ObjectManager<Gateway> {

	public List<TradeGateway> tradeGateways() {
		return objectMap.values().stream()
				.filter(gw -> gw.getDescription().getGatewayUsage() == GatewayUsage.TRADE)
				.filter(TradeGateway.class::isInstance)
				.map(TradeGateway.class::cast)
				.toList();
	}
	
	public List<MarketGateway> marketGateways() {
		return objectMap.values().stream()
				.filter(gw -> gw.getDescription().getGatewayUsage() == GatewayUsage.MARKET_DATA)
				.filter(MarketGateway.class::isInstance)
				.map(MarketGateway.class::cast)
				.toList();
	}
	
}
