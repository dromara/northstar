package org.dromara.northstar.strategy.trainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.IGatewayService;
import org.dromara.northstar.common.IModuleService;
import org.dromara.northstar.common.ObjectManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.model.ComponentAndParamsPair;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.MarketGateway;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.tester.AbstractTester;

import com.alibaba.fastjson2.JSONObject;

abstract class AbstractTrainer extends AbstractTester implements RLAgentTrainingContext{

	protected Map<String, ModuleRuntimeDescription> mrdMap = new HashMap<>();
	
	protected AbstractTrainer(ObjectManager<Gateway> gatewayMgr, ObjectManager<IModule> moduleMgr, IContractManager contractMgr,
			IGatewayService gatewayService, IModuleService moduleService) {
		super(gatewayMgr, moduleMgr, contractMgr, gatewayService, moduleService);
	}

	protected void resetModule(IModule m) {
		try {
			moduleService.modifyModule(m.getModuleDescription(), true);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	protected void createModules(TradeGateway tdGateway) {
		for(ContractSimpleInfo csi : testContracts()) {
			ModuleAccountDescription mad = ModuleAccountDescription.builder()
					.accountGatewayId(tdGateway.gatewayId())
					.bindedContracts(List.of(csi))
					.build();
			ComponentAndParamsPair strategySettings = ComponentAndParamsPair.builder()
					.componentMeta(strategy())
					.initParams(convertParams(strategyParams(csi)))
					.build();
			createModules(mad, strategySettings, csi);
		}
	}
	
	protected List<ContractSimpleInfo> testContracts(){
		return testSymbols().stream()
				.map(symbol -> symbol + "0000")
				.map(idxSymbol -> contractMgr.getContract(ChannelType.PLAYBACK, idxSymbol))
				.map(IContract::contractField)
				.map(cf -> ContractSimpleInfo.builder()
					.name(cf.getName())
					.channelType(ChannelType.PLAYBACK)
					.unifiedSymbol(cf.getUnifiedSymbol())
					.value(cf.getContractId())
					.build())
				.toList();
	}
	
	protected void createModules(ModuleAccountDescription mad, ComponentAndParamsPair strategySettings, ContractSimpleInfo csi){
		String symbolName = csi.getName();
		String symbol = csi.getUnifiedSymbol().replaceAll("\\d+.+$", "");
		for(int min : testPeriods()) {
			ModuleDescription md = ModuleDescription.builder()
					.moduleName(String.format("%s%d分钟", symbolName, min))
					.initBalance(symbolTestAmount().get(symbol))
					.usage(ModuleUsage.PLAYBACK)
					.type(ModuleType.SPECULATION)
					.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
					.numOfMinPerBar(min)
					.strategySetting(strategySettings)
					.moduleAccountSettingsDescription(List.of(mad))
					.build();
			createModule(md);
		}
	}

	protected TradeGateway createSimGateway(MarketGateway mktGateway) {
		String gatewayId = "模拟账户";
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId(gatewayId)
				.gatewayUsage(GatewayUsage.TRADE)
				.channelType(ChannelType.SIM)
				.bindedMktGatewayId(mktGateway.gatewayId())
				.settings(new JSONObject())
				.build();
		gatewayService.createGateway(gd);
		return (TradeGateway) gatewayMgr.get(Identifier.of(gatewayId));
	}
}
