package tech.quantit.northstar.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import tech.quantit.northstar.common.event.StrategyEventBus;
import tech.quantit.northstar.domain.strategy.ModuleManager;
import tech.quantit.northstar.domain.strategy.SandboxModuleManager;

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