package org.dromara.northstar.main.config;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.InternalEventBus;
import org.dromara.northstar.common.event.StrategyEventBus;
import org.dromara.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import org.dromara.northstar.main.engine.event.handler.BroadcastDispatcher;
import org.dromara.northstar.main.engine.event.handler.InternalDispatcher;
import org.dromara.northstar.main.engine.event.handler.StrategyDispatcher;
import org.dromara.northstar.main.handler.broadcast.SocketIOMessageEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 事件处理器配置
 * @author KevinHuangwl
 *
 */
@Configuration
class FastEventDispatcherConfig {

	@Bean
	NorthstarEventDispatcher internalDispatcher(FastEventEngine ee, InternalEventBus eb) {
		NorthstarEventDispatcher handler = new InternalDispatcher(eb);
		ee.addHandler(handler);
		return handler;
	}
	
	@Bean
	NorthstarEventDispatcher strategyDispatcher(FastEventEngine ee, StrategyEventBus eb) {
		NorthstarEventDispatcher handler = new StrategyDispatcher(eb);
		ee.addHandler(handler);
		return handler;
	}
	
	@Bean
	NorthstarEventDispatcher broadcastEventDispatcher(FastEventEngine ee, SocketIOMessageEngine msgEngine) {
		NorthstarEventDispatcher handler = new BroadcastDispatcher(msgEngine);
		ee.addHandler(handler);
		return handler;
	}
	
}
