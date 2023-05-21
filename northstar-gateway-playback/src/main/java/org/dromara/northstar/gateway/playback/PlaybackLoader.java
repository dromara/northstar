package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(0)
@Component
public class PlaybackLoader implements CommandLineRunner{
	
	@Autowired
	GatewayMetaProvider gatewayMetaProvider;
	
	@Autowired
	PlaybackGatewayFactory playbackGatewayFactory;
	
	
	
	@Override
	public void run(String... args) throws Exception {
		gatewayMetaProvider.add(ChannelType.PLAYBACK, new PlaybackGatewaySettings(), playbackGatewayFactory, null);
		
	}

}
