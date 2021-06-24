package tech.xuanwu.northstar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tech.xuanwu.northstar.common.event.StrategyEventBus;
import tech.xuanwu.northstar.manager.ModuleManager;
import tech.xuanwu.northstar.persistence.ModuleRepository;

@Configuration
public class StrategyEventHandlerConfig {

	@Bean
	public ModuleManager moduleManager(StrategyEventBus seb, ModuleRepository moduleRepo) {
		ModuleManager mm = new ModuleManager(moduleRepo);
		seb.register(mm);
		return mm;
	}
}
