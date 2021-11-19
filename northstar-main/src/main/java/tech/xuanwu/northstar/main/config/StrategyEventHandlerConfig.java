package tech.xuanwu.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import tech.xuanwu.northstar.common.event.StrategyEventBus;
import tech.xuanwu.northstar.main.manager.ModuleManager;
import tech.xuanwu.northstar.main.manager.SandboxModuleManager;
import tech.xuanwu.northstar.main.persistence.ModuleRepository;

@Configuration
public class StrategyEventHandlerConfig {

	@Primary
	@Bean
	public ModuleManager moduleManager(StrategyEventBus seb, ModuleRepository moduleRepo) {
		ModuleManager mm = new ModuleManager(moduleRepo);
		seb.register(mm);
		return mm;
	}
	
	@Bean
	public SandboxModuleManager sandboxModuleManager(StrategyEventBus seb, ModuleRepository moduleRepo) {
		SandboxModuleManager smm = new SandboxModuleManager(moduleRepo);
		seb.register(smm);
		return smm;
	}
}
