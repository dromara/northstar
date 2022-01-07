package tech.quantit.northstar.main.factories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import tech.quantit.northstar.common.IMailSender;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.domain.gateway.GatewayConnection;
import tech.quantit.northstar.domain.strategy.ModulePosition;
import tech.quantit.northstar.domain.strategy.ModuleStatus;
import tech.quantit.northstar.domain.strategy.RiskControlPolicy;
import tech.quantit.northstar.domain.strategy.StopLoss;
import tech.quantit.northstar.domain.strategy.StrategyModule;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.main.ExternalJarListener;
import tech.quantit.northstar.main.persistence.ModuleRepository;
import tech.quantit.northstar.main.persistence.po.ModulePositionPO;
import tech.quantit.northstar.strategy.api.DealerPolicy;
import tech.quantit.northstar.strategy.api.DynamicParamsAware;
import tech.quantit.northstar.strategy.api.EventDrivenComponent;
import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.SignalPolicy;
import tech.quantit.northstar.strategy.api.log.NorthstarLoggerFactory;
import tech.quantit.northstar.strategy.api.model.ComponentAndParamsPair;
import tech.quantit.northstar.strategy.api.model.ComponentField;
import tech.quantit.northstar.strategy.api.model.DynamicParams;
import tech.quantit.northstar.strategy.api.model.ModuleInfo;
import tech.quantit.northstar.strategy.api.model.ModulePositionInfo;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
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
	
	private IMailSender sender;
	
	private ExternalJarListener extJarListener;
	
	public StrategyModuleFactory(GatewayAndConnectionManager gatewayConnMgr, ContractManager contractMgr, ModuleRepository moduleRepo,
			IMailSender sender, ExternalJarListener extJarListener) {
		this.gatewayConnMgr = gatewayConnMgr;
		this.contractMgr = contractMgr;
		this.moduleRepo = moduleRepo;
		this.sender = sender;
		this.extJarListener = extJarListener;
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
	
	private Consumer<ModulePositionInfo> positionSavingCallback = posInfo -> {
		ModulePositionPO po = ModulePositionPO.builder()
				.moduleName(posInfo.getModuleName())
				.positions(List.of(posInfo))
				.build();
		moduleRepo.saveModulePosition(po);
	};
	
	private ModuleStatus convertStatus(ModuleInfo moduleInfo, List<ModulePositionInfo> positionInfos) {
		if(positionInfos.isEmpty()) {
			// 初始化模组持仓
			ModulePosition mp = ModulePosition.builder()
					.moduleName(moduleInfo.getModuleName())
					.direction(PositionDirectionEnum.PD_Unknown)
					.clearoutCallback(dealRecord -> moduleRepo.saveDealRecord(dealRecord))
					.positionSavingCallback(positionSavingCallback)
					.log(NorthstarLoggerFactory.getLogger(moduleInfo.getModuleName(), ModulePosition.class))
					.build();
			return new ModuleStatus(moduleInfo.getModuleName(), mp);
		} else {
			// 恢复模组持仓记录
			// TODO 暂时只考虑了简单的持仓情况
			ModulePositionInfo mpi = positionInfos.get(0);
			ContractField contract = contractMgr.getContract(mpi.getUnifiedSymbol());
			ModulePosition mp = ModulePosition.builder()
					.moduleName(moduleInfo.getModuleName())
					.openTime(mpi.getOpenTime())
					.openPrice(mpi.getOpenPrice())
					.stopLoss(new StopLoss(mpi.getPositionDir(), mpi.getStopLossPrice()))
					.openTradingDay(mpi.getOpenTradingDay())
					.volume(mpi.getVolume())
					.contract(contract)
					.clearoutCallback(dealRecord -> moduleRepo.saveDealRecord(dealRecord))
					.positionSavingCallback(positionSavingCallback)
					.direction(mpi.getPositionDir())
					.log(NorthstarLoggerFactory.getLogger(moduleInfo.getModuleName(), ModulePosition.class))
					.build();
			return new ModuleStatus(moduleInfo.getModuleName(), mp);
		}
	}
	
	private List<EventDrivenComponent> convertComponents(ModuleInfo moduleInfo) throws Exception{
		List<RiskControlRule> riskRules = new ArrayList<>();
		for(ComponentAndParamsPair pair : moduleInfo.getRiskControlRules()) {
			RiskControlRule rule = resolveComponent(pair);
			rule.setModuleName(moduleInfo.getModuleName());
			rule.setMailSender(sender);
			riskRules.add(rule);
		}
		
		RiskControlPolicy riskController = new RiskControlPolicy(moduleInfo.getModuleName(), riskRules);
		SignalPolicy signalPolicy =  resolveComponent(moduleInfo.getSignalPolicy());
		DealerPolicy dealer = resolveComponent(moduleInfo.getDealer());
		
		signalPolicy.setModuleName(moduleInfo.getModuleName());
		signalPolicy.setMailSender(sender);
		dealer.setModuleName(moduleInfo.getModuleName());
		dealer.setMailSender(sender);
		
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
		Class<?> type = null;
		Class<?> paramType = null;
		ClassLoader cl = extJarListener.getExternalClassLoader();
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
}
