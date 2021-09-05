package common;

import tech.xuanwu.northstar.strategy.common.constants.ModuleState;
import tech.xuanwu.northstar.strategy.common.model.ModuleStatus;
import tech.xuanwu.northstar.strategy.common.model.state.ModuleStateMachine;

public class TestDataFactory {

	private String name;
	
	public TestDataFactory(String name) {
		this.name = name;
	}
	
	public ModuleStatus makeModuleStatus(ModuleState state) {
		return ModuleStatus.builder()
				.stateMachine(new ModuleStateMachine(state))
				.build();
	}
}
