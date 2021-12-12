package xyz.redtorch.gateway.ctp.common;

import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.gateway.api.domain.NormalContract;
import tech.quantit.northstar.gateway.api.domain.SubscriptionManager;

public class CtpSubscriptionManager implements SubscriptionManager {

	@Override
	public boolean subscribable(NormalContract contract) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public GatewayType usedFor() {
		return GatewayType.CTP;
	}

}
