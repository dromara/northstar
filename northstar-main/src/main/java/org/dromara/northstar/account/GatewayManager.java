package org.dromara.northstar.account;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.gateway.api.Gateway;
import org.dromara.northstar.gateway.api.MarketGateway;
import org.dromara.northstar.gateway.api.TradeGateway;
import org.springframework.stereotype.Component;

/**
 * 网关管理器
 * @author KevinHuangwl
 *
 */
@Component
public class GatewayManager implements ObjectManager<Gateway> {

	private Map<Identifier, Gateway> gatewayMap = new HashMap<>();
	
	@Override
	public void add(Gateway gateway) {
		gatewayMap.put(Identifier.of(gateway.gatewayId()), gateway);
	}

	@Override
	public void remove(Identifier id) {
		gatewayMap.remove(id);
	}

	@Override
	public Gateway get(Identifier id) {
		return gatewayMap.get(id);
	}

	public List<TradeGateway> tradeGateways() {
		return gatewayMap.values().stream()
				.filter(TradeGateway.class::isInstance)
				.map(TradeGateway.class::cast)
				.toList();
	}
	
	public List<MarketGateway> marketGateways() {
		return gatewayMap.values().stream()
				.filter(MarketGateway.class::isInstance)
				.map(MarketGateway.class::cast)
				.toList();
	}
	
	public List<Gateway> allGateways(){
		return gatewayMap.values().stream().toList();
	}

	@Override
	public boolean contains(Identifier id) {
		return gatewayMap.containsKey(id);
	}
}
