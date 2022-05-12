package tech.quantit.northstar.domain.module;

import java.util.Collections;

import com.alibaba.fastjson.JSONObject;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;

public class ModuleFactory {
	
 	
	public IModule newInstance(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRuntimeDescription) {
		return null;
	}
	
	private IModuleAccountStore makeAccountStore(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRuntimeDescription) {
		if(moduleRuntimeDescription == null) {
			moduleRuntimeDescription = new ModuleRuntimeDescription(moduleDescription.getModuleName(), false, ModuleState.EMPTY, Collections.emptyMap(), new JSONObject());
		}
		return new ModuleAccountStore(moduleDescription.getModuleName(), moduleDescription.getClosingPolicy(), moduleRuntimeDescription);
	}
	
	public IModuleContext makeModuleContext() {
		
		return null;
	}
	

}
