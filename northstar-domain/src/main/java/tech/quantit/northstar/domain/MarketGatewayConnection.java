package tech.quantit.northstar.domain;

import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.model.GatewayDescription;

public class MarketGatewayConnection extends GatewayConnection {

	public MarketGatewayConnection(GatewayDescription gwDescription, InternalEventBus eventBus) {
		super(gwDescription, eventBus);
	}

}
