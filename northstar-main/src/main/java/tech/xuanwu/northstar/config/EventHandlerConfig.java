package tech.xuanwu.northstar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.common.event.PluginEventBus;
import tech.xuanwu.northstar.common.event.StrategyEventBus;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.EventEngine;
import tech.xuanwu.northstar.engine.event.EventEngine.NorthstarEventHandler;
import tech.xuanwu.northstar.engine.event.handler.BroadcastHandler;
import tech.xuanwu.northstar.engine.event.handler.InternalHandler;
import tech.xuanwu.northstar.engine.event.handler.PluginHandler;
import tech.xuanwu.northstar.engine.event.handler.StrategyHandler;

/**
 * 事件处理器配置
 * @author KevinHuangwl
 *
 */
@Configuration
public class EventHandlerConfig {

	@Bean
	public NorthstarEventHandler createInternalHandler(EventEngine ee, InternalEventBus eb) {
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
	public NorthstarEventHandler createBroadcastEventHandler(EventEngine ee, SocketIOMessageEngine msgEngine) {
		NorthstarEventHandler handler = new BroadcastHandler(msgEngine);
		ee.addHandler(handler);
		return handler;
	}
}
