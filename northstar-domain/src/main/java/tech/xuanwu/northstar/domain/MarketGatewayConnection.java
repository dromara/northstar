package tech.xuanwu.northstar.domain;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.model.GatewayDescription;

public class MarketGatewayConnection extends GatewayConnection {

	public MarketGatewayConnection(GatewayDescription gwDescription, InternalEventBus eventBus) {
		super(gwDescription, eventBus);
	}

}
