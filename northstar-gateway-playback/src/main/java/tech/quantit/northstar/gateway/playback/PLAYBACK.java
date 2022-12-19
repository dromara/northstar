package tech.quantit.northstar.gateway.playback;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.gateway.api.GatewayTypeProvider;

@DependsOn({"playbackGatewayFactory"})
@Component
public class PLAYBACK implements GatewayType, InitializingBean{

	@Autowired
	private GatewayTypeProvider gtp;
	
	@Autowired
	private PlaybackGatewayFactory factory;
	
	@Override
	public GatewayUsage[] usage() {
		return new GatewayUsage[] {GatewayUsage.MARKET_DATA};
	}

	@Override
	public boolean adminOnly() {
		return false;
	}

	@Override
	public String name() {
		return getClass().getSimpleName();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		gtp.addGatewayType(this, factory);
	}

}
