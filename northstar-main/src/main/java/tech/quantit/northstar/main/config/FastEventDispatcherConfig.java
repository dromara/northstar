package tech.quantit.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.FastEventEngine.NorthstarEventDispatcher;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.event.PlaybackEventBus;
import tech.quantit.northstar.common.event.StrategyEventBus;
import tech.quantit.northstar.main.engine.event.handler.BroadcastDispatcher;
import tech.quantit.northstar.main.engine.event.handler.InternalDispatcher;
import tech.quantit.northstar.main.engine.event.handler.PlaybackDispatcher;
import tech.quantit.northstar.main.engine.event.handler.StrategyDispatcher;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;

/**
 * 事件处理器配置
 * @author KevinHuangwl
 *
 */
@Configuration
public class FastEventDispatcherConfig {

	@Bean
	public NorthstarEventDispatcher internalDispatcher(FastEventEngine ee, InternalEventBus eb) {
		NorthstarEventDispatcher handler = new InternalDispatcher(eb);
		ee.addHandler(handler);
		return handler;
	}
	
	@Bean
	public NorthstarEventDispatcher strategyDispatcher(FastEventEngine ee, StrategyEventBus eb) {
		NorthstarEventDispatcher handler = new StrategyDispatcher(eb);
		ee.addHandler(handler);
		return handler;
	}
	
	@Bean
	public NorthstarEventDispatcher broadcastEventDispatcher(FastEventEngine ee, SocketIOMessageEngine msgEngine) {
		NorthstarEventDispatcher handler = new BroadcastDispatcher(msgEngine);
		ee.addHandler(handler);
		return handler;
	}
	
	@Bean
	public NorthstarEventDispatcher playbackDispatcher(FastEventEngine ee, PlaybackEventBus peb) {
		NorthstarEventDispatcher handler = new PlaybackDispatcher(peb);
		ee.addHandler(handler);
		return handler;
	}
}
