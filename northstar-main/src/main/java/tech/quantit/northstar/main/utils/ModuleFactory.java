package tech.quantit.northstar.main.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.model.ComponentAndParamsPair;
import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.DynamicParams;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.data.IMarketDataRepository;
import tech.quantit.northstar.data.IModuleRepository;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.module.DealCollector;
import tech.quantit.northstar.domain.module.FirstInFirstOutClosingStrategy;
import tech.quantit.northstar.domain.module.ModuleAccountStore;
import tech.quantit.northstar.domain.module.ModuleContext;
import tech.quantit.northstar.domain.module.PriorBeforeAndHedgeTodayClosingStrategy;
import tech.quantit.northstar.domain.module.PriorTodayClosingStrategy;
import tech.quantit.northstar.domain.module.TradeModule;
import tech.quantit.northstar.gateway.api.MarketGateway;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.main.ExternalJarClassLoader;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import tech.quantit.northstar.strategy.api.DynamicParamsAware;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModuleFactory {
	
	private ExternalJarClassLoader extJarLoader;
	
	private IModuleRepository moduleRepo;
	
	private IMarketDataRepository mdRepo;
	
	private IGatewayRepository gatewayRepo;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	private ContractManager contractMgr;
	
	private Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback = rt -> moduleRepo.saveRuntime(rt);
	
	private Consumer<ModuleDealRecord> onDealChangeCallback = dealRecord -> moduleRepo.saveDealRecord(dealRecord);
	
	public ModuleFactory(ExternalJarClassLoader extJarLoader, IModuleRepository moduleRepo, IGatewayRepository gatewayRepo, IMarketDataRepository mdRepo,
			GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr) {
		this.extJarLoader = extJarLoader;
		this.moduleRepo = moduleRepo;
		this.mdRepo = mdRepo;
		this.gatewayRepo = gatewayRepo;
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
	}
 	
	public IModule newInstance(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRuntimeDescription) throws Exception {
		IModuleContext ctx = makeModuleContext(moduleDescription, moduleRuntimeDescription);
		
		for(ModuleAccountDescription mad : moduleDescription.getModuleAccountSettingsDescription()) {
			TradeGateway tradeGateway = (TradeGateway) gatewayConnMgr.getGatewayById(mad.getAccountGatewayId());
			ctx.bindGatewayContracts(tradeGateway, mad.getBindedUnifiedSymbols().stream().map(contractMgr::getContract).toList());
		}
		
		Set<MarketGateway> mktGatewaySet = moduleDescription.getModuleAccountSettingsDescription().stream()
				.map(ModuleAccountDescription::getAccountGatewayId)
				.map(accGatewayId -> gatewayRepo.findById(accGatewayId))
				.map(gd -> gatewayRepo.findById(gd.getBindedMktGatewayId()))
				.map(gd -> gatewayConnMgr.getGatewayById(gd.getGatewayId()))
				.map(MarketGateway.class::cast)
				.collect(Collectors.toSet());
		
		return new TradeModule(ctx, mktGatewaySet, onRuntimeChangeCallback);
	}
	
	private IModuleAccountStore makeAccountStore(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRuntimeDescription) {
		return new ModuleAccountStore(moduleDescription.getModuleName(), moduleDescription.getClosingPolicy(), moduleRuntimeDescription, contractMgr);
	}
	
	private IModuleContext makeModuleContext(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRuntimeDescription) throws Exception {
		ComponentAndParamsPair strategyComponent = moduleDescription.getStrategySetting();
		TradeStrategy strategy = resolveComponent(strategyComponent);
		IModuleAccountStore accStore = makeAccountStore(moduleDescription, moduleRuntimeDescription);
		ClosingStrategy closingStrategy = getClosingStrategy(moduleDescription.getClosingPolicy());
		int numOfMinPerBar = moduleDescription.getNumOfMinPerBar();
		
		DealCollector dc = new DealCollector(moduleDescription.getModuleName(), moduleDescription.getClosingPolicy());
		for(ModuleAccountRuntimeDescription mard : moduleRuntimeDescription.getAccountRuntimeDescriptionMap().values()) {
			for(byte[] uncloseTradeData : mard.getPositionDescription().getUncloseTrades()) {
				dc.onTrade(TradeField.parseFrom(uncloseTradeData));
			}
		}
		int moduleBufDataSize = Math.max(100, moduleDescription.getModuleCacheDataSize());	// 至少缓存100个数据 
		return new ModuleContext(moduleDescription.getModuleName(), strategy, accStore, closingStrategy, numOfMinPerBar,
				moduleBufDataSize, dc, onRuntimeChangeCallback, onDealChangeCallback, mdRepo);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends DynamicParamsAware> T resolveComponent(ComponentAndParamsPair metaInfo) throws Exception {
		Map<String, ComponentField> fieldMap = new HashMap<>();
		for(ComponentField cf : metaInfo.getInitParams()) {
			fieldMap.put(cf.getName(), cf);
		}
		String clzName = metaInfo.getComponentMeta().getClassName();
		String paramClzName = clzName + "$InitParams";
		Class<?> type = null;
		Class<?> paramType = null;
		if(extJarLoader != null) {
			type = extJarLoader.loadClass(clzName);
			paramType = extJarLoader.loadClass(paramClzName);
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
