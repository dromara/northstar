package org.dromara.northstar.gateway.playback;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.gateway.mktdata.QuantitDataServiceManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Configuration
public class PlaybackConfig {

	
	@Bean
	PlaybackGatewayFactory playbackGatewayFactory(FastEventEngine feEngine, MarketCenter mktCenter, PlaybackContractDefProvider contractDefPvd,
			QuantitDataServiceManager dsMgr, NorthstarDataSource ds) {
		return new PlaybackGatewayFactory(feEngine, mktCenter, ds);
	}
}
