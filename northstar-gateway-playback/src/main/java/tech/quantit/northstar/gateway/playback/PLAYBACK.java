package tech.quantit.northstar.gateway.playback;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import tech.quantit.northstar.common.ChannelType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.gateway.api.GatewayChannelProvider;

@DependsOn({"playbackGatewayFactory"})
@Component
public class PLAYBACK implements ChannelType, InitializingBean{

	@Autowired
	private GatewayChannelProvider gtp;
	
	@Autowired
	private PlaybackGatewayFactory factory;
	
	@Override
	public GatewayUsage[] usage() {
		return new GatewayUsage[] {GatewayUsage.MARKET_DATA};
	}

	@Override
	public boolean allowDuplication() {
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
