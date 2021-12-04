package tech.quantit.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.event.MarketDataEventBus;
import tech.quantit.northstar.common.event.PluginEventBus;
import tech.quantit.northstar.common.event.StrategyEventBus;
import tech.quantit.northstar.main.engine.broadcast.SocketIOMessageEngine;
import tech.quantit.northstar.main.engine.event.DisruptorFastEventEngine;
import tech.quantit.northstar.main.engine.event.DisruptorFastEventEngine.WaitStrategyEnum;
import xyz.redtorch.gateway.ctp.index.IndexEngine;

/**
 * 引擎配置
 * @author KevinHuangwl
 *
 */
@Slf4j
@Configuration
public class EngineConfig {

	@Bean
	public SocketIOMessageEngine createMessageEngine(SocketIOServer server) {
		log.info("创建SocketIOMessageEngine");
		return new SocketIOMessageEngine(server);
	}
	
	@Bean
	public FastEventEngine createEventEngine() {
		log.info("创建EventEngine");
		return new DisruptorFastEventEngine(WaitStrategyEnum.BlockingWaitStrategy);
	}
	
	@Bean
	public IndexEngine createIndexEngine(FastEventEngine feEngine) {
		log.info("创建IndexEngine");
		return new IndexEngine(feEngine);
	}
	
	@Bean
	public InternalEventBus createInternalEventBus() {
		log.info("创建InternalEventBus");
		return new InternalEventBus();
	}
	
	@Bean
	public MarketDataEventBus createMarketDataEventBus() {
		log.info("创建MarketDataEventBus");
		return new MarketDataEventBus();
	}
	
	@Bean
	public PluginEventBus createPluginEventBus() {
		log.info("创建PluginEventBus");
		return new PluginEventBus();
	}
	
	@Bean
	public StrategyEventBus createStrategyEventBus() {
		log.info("创建StrategyEventBus");
		return new StrategyEventBus();
	}

}
