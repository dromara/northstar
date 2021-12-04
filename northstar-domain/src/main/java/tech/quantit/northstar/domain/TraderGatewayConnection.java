package tech.quantit.northstar.domain;

import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.model.GatewayDescription;

public class TraderGatewayConnection extends GatewayConnection{

	public TraderGatewayConnection(GatewayDescription gwDescription, InternalEventBus eventBus) {
		super(gwDescription, eventBus);
	}

}
