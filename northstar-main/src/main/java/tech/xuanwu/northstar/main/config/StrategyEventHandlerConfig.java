package tech.xuanwu.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import tech.xuanwu.northstar.common.event.StrategyEventBus;
import tech.xuanwu.northstar.domain.strategy.ModuleManager;
import tech.xuanwu.northstar.domain.strategy.SandboxModuleManager;

@Configuration
public class StrategyEventHandlerConfig {

	@Primary
	@Bean
	public ModuleManager moduleManager(StrategyEventBus seb) {
		ModuleManager mm = new ModuleManager();
		seb.register(mm);
		return mm;
	}
	
	@Bean
	public SandboxModuleManager sandboxModuleManager(StrategyEventBus seb) {
		SandboxModuleManager smm = new SandboxModuleManager();
		seb.register(smm);
		return smm;
	}
}
