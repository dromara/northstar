package common;

import com.google.common.collect.Lists;

import tech.xuanwu.northstar.common.model.ContractManager;
import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.entity.ModulePositionEntity;
import tech.xuanwu.northstar.strategy.common.model.entity.ModuleStatusEntity;

public class TestDataFactory {
	
	private String name;
	
	public TestDataFactory(String name) {
		this.name = name;
	}

	public ModuleStatus makeModuleStatus(ContractManager contractMgr, ModuleState state, String openDate, int countOfOpeningToday,
			ModulePositionEntity... modulePositions) {
		ModuleStatusEntity e = ModuleStatusEntity.builder()
				.moduleName(name)
				.state(state)
				.holdingTradingDay(openDate)
				.countOfOpeningToday(countOfOpeningToday)
				.positions(Lists.newArrayList(modulePositions))
				.build();
		
		return new ModuleStatus(e, contractMgr);
	}
	
	
}
