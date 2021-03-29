package tech.xuanwu.northstar.domain;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.model.GatewayDescription;

public class TraderGatewayConnection extends GatewayConnection{

	public TraderGatewayConnection(GatewayDescription gwDescription, InternalEventBus eventBus) {
		super(gwDescription, eventBus);
	}

}
