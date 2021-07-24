package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.state.ModuleStateMachine;

public interface StateMachineAware {
	
	void setStateMachine(ModuleStateMachine stateMachine);

}
