package tech.xuanwu.northstar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.MarketDataEventBus;
import tech.xuanwu.northstar.common.event.PluginEventBus;
import tech.xuanwu.northstar.common.event.StrategyEventBus;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine.NorthstarEventDispatcher;
import tech.xuanwu.northstar.engine.event.handler.BroadcastDispatcher;
import tech.xuanwu.northstar.engine.event.handler.InternalDispatcher;
import tech.xuanwu.northstar.engine.event.handler.MarketDataDispatcher;
import tech.xuanwu.northstar.engine.event.handler.PluginDispatcher;
import tech.xuanwu.northstar.engine.event.handler.StrategyDispatcher;

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
	
	@Bean
	public NorthstarEventDispatcher createPluginHandler(FastEventEngine ee, PluginEventBus eb) {
		NorthstarEventDispatcher handler = new PluginDispatcher(eb);
		ee.addHandler(handler);
		return handler;
	}
	
	@Bean
	public NorthstarEventDispatcher createStrategyHandler(FastEventEngine ee, StrategyEventBus eb) {
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
}
