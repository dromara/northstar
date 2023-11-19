package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Configuration
public class PlaybackConfig {

//	@Value("${northstar.data-service.baseUrl}")
//	private String baseUrl;
	
//	@Bean
//	PlaybackDataServiceManager playbackDataServiceManager(RestTemplate restTemplate) {
//		String nsdsSecret = Optional.ofNullable(System.getenv(Constants.NS_DS_SECRET)).orElse("");
//		return new PlaybackDataServiceManager(baseUrl, nsdsSecret, restTemplate, new CtpDateTimeUtil());
//	}
	
	@Bean
	PlaybackGatewayFactory playbackGatewayFactory(FastEventEngine feEngine, IContractManager contractMgr, IPlaybackRuntimeRepository pbrtRepo) {
		return new PlaybackGatewayFactory(feEngine, contractMgr, pbrtRepo);
	}
}
