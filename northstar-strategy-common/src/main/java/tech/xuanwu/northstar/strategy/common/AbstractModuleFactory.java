package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;

public abstract class AbstractModuleFactory {

	public abstract ModuleAccount newModuleAccount(double share);
	
	public abstract ModulePosition newModulePosition();
	
	public abstract ModulePosition loadModulePosition(ModuleStatus status);
	
	public abstract ModuleOrder newModuleOrder();
	
	public abstract ModuleTrade newModuleTrade();
	
}
