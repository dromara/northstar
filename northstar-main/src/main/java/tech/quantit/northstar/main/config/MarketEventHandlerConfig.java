package tech.quantit.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.MarketDataEventBus;
import tech.quantit.northstar.main.handler.data.MarketBarDataHandler;
import tech.quantit.northstar.main.persistence.BarBufferManager;

@Slf4j
@Configuration
public class MarketEventHandlerConfig {

	//////////////////////
	/* MarketData类事件 */
	/////////////////////
	@Bean
	public MarketBarDataHandler marketDataHandler(MarketDataEventBus eventBus, FastEventEngine feEngine,
			BarBufferManager bbMgr) {
		MarketBarDataHandler handler = new MarketBarDataHandler(feEngine, bbMgr);
		log.info("注册：MarketDataHandler");
		eventBus.register(handler);
		return handler;
	}
	////////////////////////
	/* MarketData类事件结束 */
	////////////////////////
}
