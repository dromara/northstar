package tech.xuanwu.northstar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.StrategyEventBus;
import tech.xuanwu.northstar.manager.ModuleManager;

@Configuration
public class StrategyEventHandlerConfig {

	@Bean
	public ModuleManager moduleManager(StrategyEventBus seb) {
		ModuleManager mm = new ModuleManager();
		seb.register(mm);
		return mm;
	}
}
