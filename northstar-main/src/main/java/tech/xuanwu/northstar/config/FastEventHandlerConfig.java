package tech.xuanwu.northstar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine;
import tech.xuanwu.northstar.engine.event.FastEventEngine.NorthstarEventHandler;
import tech.xuanwu.northstar.engine.event.handler.BroadcastHandler;
import tech.xuanwu.northstar.engine.event.handler.InternalHandler;

/**
 * 事件处理器配置
 * @author KevinHuangwl
 *
 */
@Configuration
public class FastEventHandlerConfig {

	@Bean
	public NorthstarEventHandler createInternalHandler(FastEventEngine ee, InternalEventBus eb) {
		NorthstarEventHandler handler = new InternalHandler(eb);
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
	public NorthstarEventHandler createBroadcastEventHandler(FastEventEngine ee, SocketIOMessageEngine msgEngine) {
		NorthstarEventHandler handler = new BroadcastHandler(msgEngine);
		ee.addHandler(handler);
		return handler;
	}
}
