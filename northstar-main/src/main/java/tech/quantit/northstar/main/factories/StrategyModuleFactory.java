package tech.quantit.northstar.main.factories;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.quantit.northstar.common.model.ContractManager;
import tech.quantit.northstar.domain.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.GatewayConnection;
import tech.quantit.northstar.domain.strategy.ModulePosition;
import tech.quantit.northstar.domain.strategy.ModuleStatus;
import tech.quantit.northstar.domain.strategy.RiskControlPolicy;
import tech.quantit.northstar.domain.strategy.StopLoss;
import tech.quantit.northstar.domain.strategy.StrategyModule;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.main.persistence.ModuleRepository;
import tech.quantit.northstar.strategy.api.DealerPolicy;
import tech.quantit.northstar.strategy.api.DynamicParamsAware;
import tech.quantit.northstar.strategy.api.EventDrivenComponent;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.model.ComponentAndParamsPair;
import tech.quantit.northstar.strategy.api.model.ComponentField;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import tech.quantit.northstar.strategy.api.model.ModuleInfo;
import tech.quantit.northstar.strategy.api.model.ModulePositionInfo;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 模组工厂，出厂产品未配置模组回调方法
 * @author KevinHuangwl
 *
 */
public class StrategyModuleFactory {

	private GatewayAndConnectionManager gatewayConnMgr;
	
	private ContractManager contractMgr;
	
	private ModuleRepository moduleRepo;
	
	public StrategyModuleFactory(GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr, ModuleRepository moduleRepo) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
		this.moduleRepo = moduleRepo;
	}
	
	public StrategyModule makeModule(ModuleInfo moduleInfo) throws Exception {
		return makeModule(moduleInfo, Collections.emptyList());
	}
	
	public StrategyModule makeModule(ModuleInfo moduleInfo, List<ModulePositionInfo> positionInfos) throws Exception {
		TradeGateway tradeGateway = (TradeGateway) gatewayConnMgr.getGatewayById(moduleInfo.getAccountGatewayId());
		GatewayConnection gatewayConn = gatewayConnMgr.getConnectionByGateway(tradeGateway);
		String bindedMktGatewayId = gatewayConn.getGwDescription().getBindedMktGatewayId();
		ModuleEventBus meb = new ModuleEventBus();
		ModuleStatus moduleStatus = convertStatus(moduleInfo, positionInfos, meb);
		StrategyModule module = new StrategyModule(bindedMktGatewayId, tradeGateway, moduleStatus);
		for(EventDrivenComponent component : convertComponents(moduleInfo)) {
			module.addComponent(component);
		}
		module.setEnabled(moduleInfo.isEnabled());
		return module;
	}
	
	private ModuleStatus convertStatus(ModuleInfo moduleInfo, List<ModulePositionInfo> positionInfos, ModuleEventBus meb) {
		if(positionInfos.isEmpty()) {
			ModulePosition mp = ModulePosition.builder()
					.moduleName(moduleInfo.getModuleName())
					.meb(meb)
					.clearoutCallback(dealRecord -> moduleRepo.saveDealRecord(dealRecord))
					.build();
			return new ModuleStatus(moduleInfo.getModuleName(), mp);
		} else {
			ModulePositionInfo mpi = positionInfos.get(0);
			ContractField contract = contractMgr.getContract(mpi.getUnifiedSymbol());
			ModulePosition mp = ModulePosition.builder()
					.moduleName(moduleInfo.getModuleName())
					.meb(meb)
					.openTime(mpi.getOpenTime())
					.openPrice(mpi.getOpenPrice())
					.stopLoss(new StopLoss(mpi.getPositionDir(), mpi.getStopLossPrice()))
					.openTradingDay(mpi.getOpenTradingDay())
					.volume(mpi.getVolume())
					.contract(contract)
					.clearoutCallback(dealRecord -> moduleRepo.saveDealRecord(dealRecord))
					.direction(mpi.getPositionDir())
					.build();
			return new ModuleStatus(moduleInfo.getModuleName(), mp);
		}
	}
	
	private List<EventDrivenComponent> convertComponents(ModuleInfo moduleInfo) throws Exception{
		List<RiskControlRule> riskRules = new ArrayList<>();
		for(ComponentAndParamsPair pair : moduleInfo.getRiskControlRules()) {
			RiskControlRule rule = resolveComponent(pair);
			rule.setModuleName(moduleInfo.getModuleName());
			riskRules.add(rule);
		}
		
		RiskControlPolicy riskController = new RiskControlPolicy(moduleInfo.getModuleName(), riskRules);
		SignalPolicy signalPolicy =  resolveComponent(moduleInfo.getSignalPolicy());
		DealerPolicy dealer = resolveComponent(moduleInfo.getDealer());
		
		signalPolicy.setModuleName(moduleInfo.getModuleName());
		dealer.setModuleName(moduleInfo.getModuleName());
		
		signalPolicy.setBindedContract(contractMgr.getContract(signalPolicy.bindedContractSymbol()));
		dealer.setBindedContract(contractMgr.getContract(dealer.bindedContractSymbol()));
		riskController.setBindedContract(contractMgr.getContract(dealer.bindedContractSymbol()));
		return List.of(riskController, signalPolicy, dealer);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends DynamicParamsAware> T resolveComponent(ComponentAndParamsPair metaInfo) throws Exception {
		Map<String, ComponentField> fieldMap = new HashMap<>();
		for(ComponentField cf : metaInfo.getInitParams()) {
			fieldMap.put(cf.getName(), cf);
		}
		String clzName = metaInfo.getComponentMeta().getClassName();
		String paramClzName = clzName + "$InitParams";
		Class<?> type = Class.forName(clzName);
		Class<?> paramType = Class.forName(paramClzName);
		DynamicParamsAware obj = (DynamicParamsAware) type.getDeclaredConstructor().newInstance();
		DynamicParams paramObj = (DynamicParams) paramType.getDeclaredConstructor().newInstance();
		paramObj.resolveFromSource(fieldMap);
		obj.initWithParams(paramObj);
		return (T) obj;
	}
}
