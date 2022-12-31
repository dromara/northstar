package tech.quantit.northstar.gateway.ctp;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.ChannelType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.gateway.api.GatewayChannelProvider;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;

@Component
public class CTP_SIM implements ChannelType, InitializingBean {
	
	@Autowired
	private GatewayChannelProvider gtp;
	
	@Autowired
	private CtpSimGatewayFactory factory;

	@Override
	public GatewayUsage[] usage() {
		return new GatewayUsage[] {GatewayUsage.MARKET_DATA, GatewayUsage.TRADE};
	}

	@Override
	public boolean adminOnly() {
		return true;
	}

	@Override
	public String name() {
		return getClass().getSimpleName();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		gtp.addGatewayChannel(this, factory);
	}

}
