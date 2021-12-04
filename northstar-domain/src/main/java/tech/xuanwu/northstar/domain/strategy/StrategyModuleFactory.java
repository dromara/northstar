package tech.xuanwu.northstar.domain.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.domain.GatewayAndConnectionManager;
import tech.xuanwu.northstar.domain.GatewayConnection;
import tech.xuanwu.northstar.gateway.api.TradeGateway;
import tech.xuanwu.northstar.strategy.api.DealerPolicy;
import tech.xuanwu.northstar.strategy.api.DynamicParamsAware;
import tech.xuanwu.northstar.strategy.api.EventDrivenComponent;
import tech.xuanwu.northstar.strategy.api.RiskControlRule;
import tech.xuanwu.northstar.strategy.api.SignalPolicy;
import tech.xuanwu.northstar.strategy.api.model.ComponentAndParamsPair;
import tech.xuanwu.northstar.strategy.api.model.ComponentField;
import tech.xuanwu.northstar.strategy.api.model.DynamicParams;
import tech.xuanwu.northstar.strategy.api.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.api.model.ModulePositionInfo;
import xyz.redtorch.pb.CoreField.ContractField;

/**
 * 模组工厂，出厂产品未配置模组回调方法
 * @author KevinHuangwl
 *
 */
public class StrategyModuleFactory {

	private GatewayAndConnectionManager gatewayConnMgr;
	
	private ContractManager contractMgr;
	
	public StrategyModuleFactory(GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
	}
	
	public StrategyModule makeModule(ModuleInfo moduleInfo) throws Exception {
		return makeModule(moduleInfo, Collections.emptyList());
	}
	
	public StrategyModule makeModule(ModuleInfo moduleInfo, List<ModulePositionInfo> positionInfos) throws Exception {
		TradeGateway tradeGateway = (TradeGateway) gatewayConnMgr.getGatewayById(moduleInfo.getAccountGatewayId());
		GatewayConnection gatewayConn = gatewayConnMgr.getConnectionByGateway(tradeGateway);
		String bindedMktGatewayId = gatewayConn.getGwDescription().getBindedMktGatewayId();
		ModuleStatus moduleStatus = convertStatus(moduleInfo, positionInfos);
		StrategyModule module = new StrategyModule(bindedMktGatewayId, tradeGateway, moduleStatus);
		for(EventDrivenComponent component : convertComponents(moduleInfo)) {
			module.addComponent(component);
		}
		module.setEnabled(moduleInfo.isEnabled());
		return module;
	}
	
	private ModuleStatus convertStatus(ModuleInfo moduleInfo, List<ModulePositionInfo> positionInfos) {
		ModuleStatus status = new ModuleStatus(moduleInfo.getModuleName());
		for(ModulePositionInfo mpi : positionInfos) {
			ContractField contract = contractMgr.getContract(mpi.getUnifiedSymbol());
			status.addPosition(new ModulePosition(mpi, contract));
		}
		return status;
	}
	
	private List<EventDrivenComponent> convertComponents(ModuleInfo moduleInfo) throws Exception{
		List<RiskControlRule> riskRules = new ArrayList<>();
		for(ComponentAndParamsPair pair : moduleInfo.getRiskControlRules()) {
			riskRules.add(resolveComponent(pair));
		}
		
		RiskControlPolicy riskController = new RiskControlPolicy(moduleInfo.getModuleName(), riskRules);
		SignalPolicy signalPolicy =  resolveComponent(moduleInfo.getSignalPolicy());
		DealerPolicy dealer = resolveComponent(moduleInfo.getDealer());
		
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
