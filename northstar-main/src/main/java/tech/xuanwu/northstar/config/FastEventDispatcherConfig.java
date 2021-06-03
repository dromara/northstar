package tech.xuanwu.northstar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.MarketDataEventBus;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine.NorthstarEventDispatcher;
import tech.xuanwu.northstar.engine.event.handler.BroadcastDispatcher;
import tech.xuanwu.northstar.engine.event.handler.InternalDispatcher;
import tech.xuanwu.northstar.engine.event.handler.MarketDataDispatcher;

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
	public NorthstarEventDispatcher marketDataDispatcher(FastEventEngine ee, MarketDataEventBus mdeb) {
		NorthstarEventDispatcher handler = new MarketDataDispatcher(mdeb);
		ee.addHandler(handler);
		return handler;
	}
	
//	@Bean
//	public NorthstarEventHandler createPluginHandler(EventEngine ee, PluginEventBus eb) {
//		NorthstarEventHandler handler = new PluginHandler(eb);
//		ee.addHandler(handler);
//		return handler;
//	}
//	
//	@Bean
//	public NorthstarEventHandler createStrategyHandler(EventEngine ee, StrategyEventBus eb) {
//		NorthstarEventHandler handler = new StrategyHandler(eb);
//		ee.addHandler(handler);
//		return handler;
//	}
	
	@Bean
	public NorthstarEventDispatcher broadcastEventDispatcher(FastEventEngine ee, SocketIOMessageEngine msgEngine) {
		NorthstarEventDispatcher handler = new BroadcastDispatcher(msgEngine);
		ee.addHandler(handler);
		return handler;
	}
}
