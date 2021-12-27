package tech.quantit.northstar.strategy.api;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import tech.quantit.northstar.strategy.api.constant.ModuleState;
import tech.quantit.northstar.strategy.api.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventBus;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.log.NorthstarLoggerFactory;
import tech.quantit.northstar.strategy.api.model.Signal;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

public abstract class AbstractSignalPolicy implements SignalPolicy {

	protected ModuleEventBus moduleEventBus;
	
	protected ModuleState currentState;
	
	protected String bindedUnifiedSymbol;
	
	protected int periodMins;
	
	protected int numOfRefData;
	
	protected ContractField bindedContract;
	
	protected Map<String, Indicator> indicatorMap = new HashMap<>();

	private String moduleName;
	
	private boolean moduleEnabled;
	
	protected Logger log;
	
	protected void emit(SignalOperation signalOperation) {
		emit(signalOperation, 0, 0);
	}
	
	protected void emit(SignalOperation signalOperation, double price, int ticksOfStopLoss) {
		if(!isActive()) {
			throw new IllegalStateException("当前状态下 [" + currentState + "] 不能发交易信号。");
		}
		moduleEventBus.post(new ModuleEvent<>(ModuleEventType.SIGNAL_CREATED, new Signal(signalOperation, price, ticksOfStopLoss)));
		if(log.isInfoEnabled()) {			
			log.info("[{}->{}] 发出交易信号：{} {} 止损{}个TICK", getModuleName(), name(), signalOperation, price, ticksOfStopLoss);
		}
	}
	
	@Override
	public void onEvent(ModuleEvent<?> moduleEvent) {
		if(moduleEvent.getEventType() == ModuleEventType.MODULE_TOGGLE) {
			moduleEnabled = (boolean) moduleEvent.getData();
			log.info("[{}] 模组当前状态:[{}]", moduleName, moduleEnabled ? "启用" : "停用");
		}
	}

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
	
	@Override
	public void setBindedContract(ContractField contract) {
		bindedContract = contract;
	}

	@Override
	public void onTick(TickField tick) {
		if(tick.getUnifiedSymbol().equals(bindedUnifiedSymbol) && moduleEnabled) {
			handleTick(tick);
		}
	}

	@Override
	public void onBar(BarField bar) {
		if(bar.getUnifiedSymbol().equals(bindedUnifiedSymbol) && moduleEnabled) {
			handleBar(bar);
		}
	}
	
	@Override
	public void setModuleName(String name) {
		this.moduleName = name;
		log = NorthstarLoggerFactory.getLogger(name, getClass()); 
	}

	@Override
	public String getModuleName() {
		return moduleName;
	}
	
	@Override
	public int periodMins() {
		return periodMins;
	}

	@Override
	public int numOfRefData() {
		return numOfRefData;
	}

	/**
	 * 
	 * @param tick
	 */
	protected abstract void handleTick(TickField tick);
	
	/**
	 * 
	 * @param bar
	 */
	protected abstract void handleBar(BarField bar);
	
}
