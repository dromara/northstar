package org.dromara.northstar.gateway.playback;

import java.util.Optional;

import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.playback.utils.CtpDateTimeUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PlaybackConfig {

	@Value("${northstar.data-service.baseUrl}")
	private String baseUrl;
	
	@Bean
	PlaybackDataServiceManager playbackDataServiceManager(RestTemplate restTemplate) {
		String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
		return new PlaybackDataServiceManager(baseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil());
	}
	
	@Bean
	PlaybackGatewayFactory playbackGatewayFactory(FastEventEngine feEngine, IContractManager contractMgr,
			IPlaybackRuntimeRepository pbrtRepo, PlaybackDataServiceManager dsMgr) {
		return new PlaybackGatewayFactory(feEngine, contractMgr, pbrtRepo, dsMgr);
	}
}
