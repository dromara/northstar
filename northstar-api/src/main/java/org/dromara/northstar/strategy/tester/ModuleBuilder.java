package org.dromara.northstar.strategy.tester;

import org.dromara.northstar.common.IModuleService;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.strategy.IModule;

public class ModuleBuilder {

	private ObjectManager<IModule> moduleMgr;
	
	private IModuleService moduleService;
	
	public ModuleBuilder(IModuleService moduleService, ObjectManager<IModule> moduleMgr) {
		this.moduleService = moduleService;
		this.moduleMgr = moduleMgr;
	}
	
	public IModule createModule(ModuleDescription md) {
		try {
			moduleService.createModule(md);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return moduleMgr.get(Identifier.of(md.getModuleName()));
	}
}
