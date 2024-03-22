package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.gateway.mktdata.QuantitDataServiceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

@EnableRetry
@Configuration
public class PlaybackConfig {

	@Value("${northstar.data-service.baseUrl}")
	private String baseUrl;
	
	@Bean
	RestTemplate restTemplate() {
		return new RestTemplateBuilder()
				.rootUri(baseUrl)
				.defaultHeader("Authorization", String.format("Bearer %s", System.getenv("NS_DS_SECRET")))
				.build();
	}
	
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
