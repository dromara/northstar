package tech.xuanwu.northstar.engine.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.InternalEventBus;
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
public class HandlerConfig {

	@Bean
	@Autowired
	public NorthstarEventHandler createInternalHandler(EventEngine ee, InternalEventBus eb) {
		return new InternalHandler(ee, eb);	
	}
	
	@Bean
	@Autowired
	public NorthstarEventHandler createPluginHandler(EventEngine ee, InternalEventBus eb) {
		return new PluginHandler(ee, eb);
	}
	
	@Bean
	@Autowired
	public NorthstarEventHandler createStrategyHandler(EventEngine ee, InternalEventBus eb) {
		return new StrategyHandler(ee, eb);
	}
	
	@Bean
	@Autowired
	public NorthstarEventHandler createBroadcastEventHandler(EventEngine ee, SocketIOMessageEngine msgEngine) {
		return new BroadcastHandler(ee, msgEngine);
	}
}
