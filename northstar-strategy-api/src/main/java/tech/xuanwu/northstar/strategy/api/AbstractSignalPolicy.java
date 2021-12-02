package tech.xuanwu.northstar.strategy.api;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.api.constant.ModuleState;
import tech.xuanwu.northstar.strategy.api.constant.Signal;
import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventType;

@Slf4j
public abstract class AbstractSignalPolicy implements SignalPolicy {

	protected ModuleEventBus moduleEventBus;
	
	protected ModuleState currentState;
	
	protected String bindedUnifiedSymbol;
	
	protected void emit(Signal signal) {
		if(!isActive()) {
			throw new IllegalStateException("当前状态下 [" + currentState + "] 不能发交易信号。");
		}
		moduleEventBus.post(new ModuleEvent<>(ModuleEventType.SIGNAL_CREATED, signal));
		log.info("[{}] 发出交易信号：{}", name(), signal);
	}
	
	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {/* 不作处理 */}

	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		this.moduleEventBus = moduleEventBus;
	}
	
	protected boolean isActive() {
		return currentState == ModuleState.EMPTY || currentState  == ModuleState.HOLDING_LONG || currentState == ModuleState.HOLDING_SHORT;
	}

	@Override
	public void onChange(ModuleState state) {
		currentState = state;
	}

	@Override
	public String bindedContractSymbol() {
		return bindedUnifiedSymbol;
	}
	
}
