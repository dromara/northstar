package tech.xuanwu.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.event.MarketDataEventBus;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.engine.index.IndexEngine;
import tech.xuanwu.northstar.main.handler.data.IndexContractHandler;
import tech.xuanwu.northstar.main.handler.data.MarketBarDataHandler;
import tech.xuanwu.northstar.main.persistence.BarBufferManager;

@Slf4j
@Configuration
public class MarketEventHandlerConfig {

	//////////////////////
	/* MarketData类事件 */
	/////////////////////
	@Bean
	public IndexContractHandler indexContractHandler(MarketDataEventBus eventBus, IndexEngine idxEngine) {
		IndexContractHandler handler = new IndexContractHandler(idxEngine);
		log.info("注册：IndexContractHandler");
		eventBus.register(handler);
		return handler;
	}

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
