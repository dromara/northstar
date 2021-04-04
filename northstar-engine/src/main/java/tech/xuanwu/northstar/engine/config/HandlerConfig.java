package tech.xuanwu.northstar.engine.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.InternalEventBus;
import tech.xuanwu.northstar.engine.broadcast.SocketIOMessageEngine;
import tech.xuanwu.northstar.engine.event.EventEngine;
import tech.xuanwu.northstar.engine.event.EventEngine.NorthstarEventHandler;
import tech.xuanwu.northstar.engine.event.handler.BroadcastEventHandler;
import tech.xuanwu.northstar.engine.event.handler.InternalEventHandler;
import tech.xuanwu.northstar.engine.event.handler.PluginEventHandler;

/**
 * 事件处理器配置
 * @author KevinHuangwl
 *
 */
@Configuration
public class HandlerConfig {

	@Bean
	@Autowired
	public NorthstarEventHandler createInternalEventHandler(EventEngine ee, InternalEventBus eb) {
		return new InternalEventHandler(ee, eb);	
	}
	
	@Bean
	@Autowired
	public NorthstarEventHandler createPluginEventHandler(EventEngine ee, InternalEventBus eb) {
		return new PluginEventHandler(ee, eb);
	}
	
	@Bean
	@Autowired
	public NorthstarEventHandler createBroadcastEventHandler(EventEngine ee, SocketIOMessageEngine msgEngine) {
		return new BroadcastEventHandler(ee, msgEngine);
	}
}
