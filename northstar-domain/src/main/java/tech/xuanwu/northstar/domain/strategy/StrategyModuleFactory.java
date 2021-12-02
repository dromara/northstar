package tech.xuanwu.northstar.domain.strategy;

import java.util.Collections;
import java.util.List;

import tech.xuanwu.northstar.domain.GatewayAndConnectionManager;
import tech.xuanwu.northstar.strategy.api.model.ModuleInfo;
import tech.xuanwu.northstar.strategy.api.model.ModulePositionInfo;

public class StrategyModuleFactory {

	private GatewayAndConnectionManager gatewayConnMgr;
	
	public StrategyModuleFactory(GatewayAndConnectionManager gatewayConnMgr) {
		this.gatewayConnMgr = gatewayConnMgr;
	}
	
	public StrategyModule makeModule(ModuleInfo moduleInfo) {
		return makeModule(moduleInfo, Collections.emptyList());
	}
	
	public StrategyModule makeModule(ModuleInfo moduleInfo, List<ModulePositionInfo> positionInfos) {
		return null;
	}
}
