package tech.quantit.northstar.gateway.tiger;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.gateway.api.GatewayTypeProvider;

@DependsOn({"tigerGatewayFactory"})
@Component
public class TIGER implements GatewayType, InitializingBean{

	@Autowired
	GatewayTypeProvider gtp;
	
	@Autowired
	TigerGatewayFactory factory;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		gtp.addGatewayType(this, factory);
	}

	@Override
	public GatewayUsage[] usage() {
		return new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE};
	}

	@Override
	public boolean adminOnly() {
		return false;
	}

	@Override
	public String name() {
		return getClass().getSimpleName();
	}
}
