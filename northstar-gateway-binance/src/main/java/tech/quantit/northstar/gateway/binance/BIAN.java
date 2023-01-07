package tech.quantit.northstar.gateway.binance;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;


@Component
public class BIAN implements GatewayType, InitializingBean {
	
	@Autowired
	private BinanceGatewayFactory factory;

	@Override
	public void afterPropertiesSet() throws Exception {
		
	}

	@Override
	public GatewayUsage[] usage() {
		return new GatewayUsage[]{GatewayUsage.MARKET_DATA, GatewayUsage.TRADE};
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
