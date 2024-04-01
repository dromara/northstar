package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.gateway.mktdata.QuantitDataServiceManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

@EnableRetry
@Configuration
public class PlaybackConfig {

	@Bean
	QuantitDataServiceManager dataService(RestTemplate restTemplate) {
		return new QuantitDataServiceManager(restTemplate);
	}
	
	@Bean
	NorthstarDataSource playbackDataServiceManager(QuantitDataServiceManager dataService) {
		return new NorthstarDataSource(dataService);
	}
	
	@Bean
	PlaybackGatewayFactory playbackGatewayFactory(FastEventEngine feEngine, IContractManager contractMgr, IPlaybackRuntimeRepository pbrtRepo) {
		return new PlaybackGatewayFactory(feEngine, contractMgr, pbrtRepo);
	}
}
