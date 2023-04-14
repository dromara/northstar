package org.dromara.northstar.main.config;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.InternalEventBus;
import org.dromara.northstar.common.event.PlaybackEventBus;
import org.dromara.northstar.common.event.StrategyEventBus;
import org.dromara.northstar.main.engine.event.DisruptorFastEventEngine;
import org.dromara.northstar.main.engine.event.DisruptorFastEventEngine.WaitStrategyEnum;
import org.dromara.northstar.main.handler.broadcast.SocketIOMessageEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.SocketIOServer;

import lombok.extern.slf4j.Slf4j;

/**
 * 引擎配置
 * @author KevinHuangwl
 *
 */
@Slf4j
@Configuration
class EngineConfig {

	@Bean
	SocketIOMessageEngine messageEngine(SocketIOServer server) {
		log.debug("创建SocketIOMessageEngine");
		return new SocketIOMessageEngine(server);
	}
	
	@Bean
	FastEventEngine eventEngine() {
		log.debug("创建EventEngine");
		return new DisruptorFastEventEngine(WaitStrategyEnum.BlockingWaitStrategy);
	}
	
	@Bean
	InternalEventBus internalEventBus() {
		log.debug("创建InternalEventBus");
		return new InternalEventBus();
	}
	
	@Bean
	StrategyEventBus strategyEventBus() {
		log.debug("创建StrategyEventBus");
		return new StrategyEventBus();
	}

	@Bean 
	PlaybackEventBus playbackEventBus() {
		log.debug("创建PlaybackEventBus");
		return new PlaybackEventBus();
	}
}
