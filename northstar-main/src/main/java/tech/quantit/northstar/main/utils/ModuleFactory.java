package tech.quantit.northstar.main.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.model.ComponentAndParamsPair;
import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.domain.module.FirstInFirstOutClosingStrategy;
import tech.quantit.northstar.domain.module.ModuleAccountStore;
import tech.quantit.northstar.domain.module.ModuleContext;
import tech.quantit.northstar.domain.module.PriorBeforeAndHedgeTodayClosingStrategy;
import tech.quantit.northstar.domain.module.PriorTodayClosingStrategy;
import tech.quantit.northstar.domain.module.TradeModule;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import tech.quantit.northstar.strategy.api.DynamicParamsAware;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.TradeStrategy;

public class ModuleFactory {
	
	private ClassLoader loader;
	
	private IModuleRepository moduleRepo;
	
	public ModuleFactory(ClassLoader loader, IModuleRepository moduleRepo) {
		this.loader = loader;
		this.moduleRepo = moduleRepo;
	}
 	
	public IModule newInstance(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRuntimeDescription) throws Exception {
		IModuleContext ctx = makeModuleContext(moduleDescription, moduleRuntimeDescription);
		Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback = (rt) -> {
			moduleRepo.saveRuntime(rt);
		};
		Consumer<ModuleDealRecord> onDealChangeCallback = (dealRecord) -> {
			moduleRepo.saveDealRecord(dealRecord);
		};
		return new TradeModule(moduleDescription.getModuleName(), ctx, moduleDescription.getClosingPolicy(), onRuntimeChangeCallback, onDealChangeCallback);
	}
	
	private IModuleAccountStore makeAccountStore(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRuntimeDescription) {
		return new ModuleAccountStore(moduleDescription.getModuleName(), moduleDescription.getClosingPolicy(), moduleRuntimeDescription);
	}
	
	private IModuleContext makeModuleContext(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRuntimeDescription) throws Exception {
		ComponentAndParamsPair strategyComponent = moduleDescription.getStrategySetting();
		TradeStrategy strategy = resolveComponent(strategyComponent);
		IModuleAccountStore accStore = makeAccountStore(moduleDescription, moduleRuntimeDescription);
		ClosingStrategy closingStrategy = getClosingStrategy(moduleDescription.getClosingPolicy());
		int numOfMinPerBar = moduleDescription.getNumOfMinPerBar();
		return new ModuleContext(strategy, accStore, closingStrategy, numOfMinPerBar);
	}
	
	private <T extends DynamicParamsAware> T resolveComponent(ComponentAndParamsPair metaInfo) throws Exception {
		Map<String, ComponentField> fieldMap = new HashMap<>();
		for(ComponentField cf : metaInfo.getInitParams()) {
			fieldMap.put(cf.getName(), cf);
		}
		String clzName = metaInfo.getComponentMeta().getClassName();
		String paramClzName = clzName + "$InitParams";
		Class<?> type = null;
		Class<?> paramType = null;
		ClassLoader cl = loader;
		if(cl != null) {
			type = cl.loadClass(clzName);
			paramType = cl.loadClass(paramClzName);
		}
		if(type == null) {
			type = Class.forName(clzName);
			paramType = Class.forName(paramClzName);
		}
		
		DynamicParamsAware obj = (DynamicParamsAware) type.getDeclaredConstructor().newInstance();
		DynamicParams paramObj = (DynamicParams) paramType.getDeclaredConstructor().newInstance();
		paramObj.resolveFromSource(fieldMap);
		obj.initWithParams(paramObj);
		return (T) obj;
	}

	private ClosingStrategy getClosingStrategy(ClosingPolicy closingPolicy) {
		return switch(closingPolicy) {
		case FIFO -> new FirstInFirstOutClosingStrategy();
		case PRIOR_BEFORE_HEGDE_TODAY -> new PriorBeforeAndHedgeTodayClosingStrategy();
		case PRIOR_TODAY -> new PriorTodayClosingStrategy();
		default -> throw new IllegalStateException("没有该类型的处理：" + closingPolicy);
		};
	}
}
