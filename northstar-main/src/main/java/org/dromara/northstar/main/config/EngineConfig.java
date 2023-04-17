package org.dromara.northstar.main.config;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import org.dromara.northstar.event.BroadcastDispatcher;
import org.dromara.northstar.event.DisruptorFastEventEngine;
import org.dromara.northstar.event.InternalDispatcher;
import org.dromara.northstar.event.DisruptorFastEventEngine.WaitStrategyEnum;
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
	FastEventEngine eventEngine() {
		log.debug("创建EventEngine");
		return new DisruptorFastEventEngine(WaitStrategyEnum.BlockingWaitStrategy);
	}
	
	@Bean
	NorthstarEventDispatcher broadcastEventDispatcher(FastEventEngine ee, SocketIOServer socketServer) {
		return new BroadcastDispatcher(socketServer);
	}
	
	@Bean
	NorthstarEventDispatcher internalDispatcher(FastEventEngine ee) {
		return new InternalDispatcher();
	}
}
