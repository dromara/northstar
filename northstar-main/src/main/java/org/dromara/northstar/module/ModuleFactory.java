package org.dromara.northstar.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.dromara.northstar.ExternalJarClassLoader;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.model.ComponentAndParamsPair;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.api.IContractManager;
import org.dromara.northstar.gateway.api.MarketGateway;
import org.dromara.northstar.gateway.api.TradeGateway;
import org.dromara.northstar.gateway.api.domain.contract.Contract;
import org.dromara.northstar.strategy.api.ClosingStrategy;
import org.dromara.northstar.strategy.api.DynamicParamsAware;
import org.dromara.northstar.strategy.api.IModule;
import org.dromara.northstar.strategy.api.IModuleAccountStore;
import org.dromara.northstar.strategy.api.IModuleContext;
import org.dromara.northstar.strategy.api.TradeStrategy;
import org.dromara.northstar.strategy.api.utils.trade.DealCollector;
import org.dromara.northstar.support.notification.MailDeliveryManager;

import xyz.redtorch.pb.CoreField.TradeField;

public class ModuleFactory {
	
	private ExternalJarClassLoader extJarLoader;
	
	private IModuleRepository moduleRepo;
	
	private IGatewayRepository gatewayRepo;
	
	private GatewayManager gatewayMgr;
	
	private IContractManager contractMgr;
	
	private MailDeliveryManager mailMgr;
	
	private Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback = rt -> moduleRepo.saveRuntime(rt);
	
	private Consumer<ModuleDealRecord> onDealChangeCallback = dealRecord -> moduleRepo.saveDealRecord(dealRecord);
	
	public ModuleFactory(ExternalJarClassLoader extJarLoader, IModuleRepository moduleRepo, IGatewayRepository gatewayRepo, 
			GatewayManager gatewayMgr, IContractManager contractMgr, MailDeliveryManager mailMgr) {
		this.extJarLoader = extJarLoader;
		this.moduleRepo = moduleRepo;
		this.gatewayRepo = gatewayRepo;
		this.gatewayMgr = gatewayMgr;
		this.contractMgr = contractMgr;
		this.mailMgr = mailMgr;
	}
 	
	public IModule newInstance(ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRuntimeDescription) throws Exception {
		IModuleContext ctx = makeModuleContext(moduleDescription, moduleRuntimeDescription);
		
		for(ModuleAccountDescription mad : moduleDescription.getModuleAccountSettingsDescription()) {
			TradeGateway tradeGateway = (TradeGateway) gatewayMgr.get(Identifier.of(mad.getAccountGatewayId()));
			List<Contract> contracts = mad.getBindedContracts()
					.stream()
					.map(contractSimple -> contractMgr.getContract(Identifier.of(contractSimple.getValue())))
					.toList();
			ctx.bindGatewayContracts(tradeGateway, contracts);
		}
		
		Set<MarketGateway> mktGatewaySet = moduleDescription.getModuleAccountSettingsDescription().stream()
				.map(ModuleAccountDescription::getAccountGatewayId)
				.map(accGatewayId -> gatewayRepo.findById(accGatewayId))
				.map(gd -> gatewayRepo.findById(gd.getBindedMktGatewayId()))
				.map(gd -> gatewayMgr.get(Identifier.of(gd.getGatewayId())))
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
		strategy.setComputedState(moduleRuntimeDescription.getDataState());
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
		if(moduleDescription.getUsage() == ModuleUsage.PLAYBACK) {
			IModuleAccountStore pbAccStore = new ModuleAccountStore(moduleDescription.getModuleName(), moduleDescription.getClosingPolicy(), moduleRuntimeDescription, contractMgr);
			return new ModulePlaybackContext(moduleDescription.getModuleName(), strategy, pbAccStore, numOfMinPerBar, moduleBufDataSize, dc,
					onRuntimeChangeCallback, onDealChangeCallback);
		}
		return new ModuleContext(moduleDescription.getModuleName(), strategy, accStore, closingStrategy, numOfMinPerBar,
				moduleBufDataSize, dc, onRuntimeChangeCallback, onDealChangeCallback, mailMgr);
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
