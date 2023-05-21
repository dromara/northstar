package org.dromara.northstar.module;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.strategy.IModule;
import org.springframework.stereotype.Component;

/**
 * 模组管理器
 * @author KevinHuangwl
 *
 */
@Component
public class ModuleManager implements ObjectManager<IModule>{

	private ConcurrentMap<Identifier, IModule> moduleMap = new ConcurrentHashMap<>();
	
	@Override
	public void add(IModule module) {
		moduleMap.put(Identifier.of(module.getName()), module);
	}

	@Override
	public void remove(Identifier id) {
		moduleMap.remove(id);
	}

	@Override
	public IModule get(Identifier id) {
		return moduleMap.get(id);
	}

	@Override
	public boolean contains(Identifier id) {
		return moduleMap.containsKey(id);
	}

	public List<IModule> allModules(){
		return moduleMap.values().stream().toList();
	}

	@Override
	public List<IModule> findAll() {
		return allModules();
	}
}
