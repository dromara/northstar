package tech.xuanwu.northstar.strategy.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.domain.GatewayAndConnectionManager;
import tech.xuanwu.northstar.strategy.common.model.GenericRiskController;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleInfo;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentAndParamsPair;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentField;
import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;

public class StrategyModuleFactory {

	private ContractManager contractMgr;
	
	private GatewayAndConnectionManager gatewayConnMgr;
	
	public StrategyModuleFactory(ContractManager contractMgr, GatewayAndConnectionManager gatewayConnMgr) {
		this.contractMgr = contractMgr;
		this.gatewayConnMgr = gatewayConnMgr;
	}
	
	public StrategyModule makeModule(ModuleInfo moduleInfo, ModuleStatus moduleStatus) throws Exception {
		List<RiskControlRule> riskRules = new ArrayList<>();
		for(ComponentAndParamsPair pair : moduleInfo.getRiskControlRules()) {
			riskRules.add(resolveComponent(pair));
		}
		
		RiskController riskController = new GenericRiskController(riskRules);
		SignalPolicy signalPolicy =  resolveComponent(moduleInfo.getSignalPolicy());
		Dealer dealer = resolveComponent(moduleInfo.getDealer());
		
		signalPolicy.setModuleStatus(moduleStatus);
		signalPolicy.setContractManager(contractMgr);
		dealer.setModuleStatus(moduleStatus);
		dealer.setContractManager(contractMgr);
		return StrategyModule.builder()
				.accountId(moduleInfo.getAccountGatewayId())
				.gatewayConnMgr(gatewayConnMgr)
				.status(moduleStatus)
				.disabled(!moduleInfo.isEnabled())
				.dealer(dealer)
				.signalPolicy(signalPolicy)
				.riskController(riskController)
				.build();
	}
	
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
