package tech.xuanwu.northstar.strategy.common;

import java.util.List;

import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.entity.TradeDescriptionEntity;

public abstract class AbstractModuleFactory {

	public abstract ModulePosition newModulePosition();
	
	public abstract ModulePosition loadModulePosition(ModuleStatus status);
	
	public abstract ModuleTrade newModuleTrade();
	
	public abstract ModuleTrade loadModuleTrade(List<TradeDescriptionEntity> originTradeList);
	
}
