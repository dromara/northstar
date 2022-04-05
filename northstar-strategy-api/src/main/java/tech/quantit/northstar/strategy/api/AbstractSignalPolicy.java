package tech.quantit.northstar.strategy.api;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.google.common.eventbus.EventBus;

import tech.quantit.northstar.common.IMailSender;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.model.Message;
import tech.quantit.northstar.strategy.api.event.ModuleEvent;
import tech.quantit.northstar.strategy.api.event.ModuleEventType;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.log.NorthstarLoggerFactory;
import tech.quantit.northstar.strategy.api.model.Signal;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

public abstract class AbstractSignalPolicy implements SignalPolicy {

	protected EventBus moduleEventBus;
	
	protected ModuleState currentState;
	
	protected String bindedUnifiedSymbol;
	
	protected int periodMins;
	
	protected int numOfRefData;
	
	protected ContractField bindedContract;
	
	protected Map<String, Indicator> indicatorMap = new HashMap<>();

	private String moduleName;
	
	private boolean moduleEnabled;
	
	protected Logger log;
	
	private IMailSender sender;
	
	protected long lastActionTime;
	
	protected TickField lastTick;
	
	/**
	 * 发送信号
	 * @param signal
	 */
	protected void emit(Signal signal, long actionTime) {
		if(!isActive()) {
			throw new IllegalStateException("当前状态下 [" + currentState + "] 不能发交易信号。");
		}
		lastActionTime = actionTime;
		moduleEventBus.post(new ModuleEvent<>(ModuleEventType.SIGNAL_CREATED, signal));
		if(log.isInfoEnabled()) {			
			log.info("[{}->{}] 发出交易信号：{}", getModuleName(), name(), signal.getSignalOperation());
			if(signal.getSignalPrice() > 0) 				
				log.info("[{}->{}] 信号价：{}", getModuleName(), name(), signal.getSignalPrice());
			if(signal.getTicksToStop() > 0)
				log.info("[{}->{}] 止损：{}个价位", getModuleName(), name(), signal.getTicksToStop());
			if(signal.getVolume() > 0)
				log.info("[{}->{}] 下单量：{}手", getModuleName(), name(), signal.getVolume());
		}
	}
	
	/**
	 * 发送消息给订阅用户
	 * @param msg
	 */
	public void sendMessage(Message msg) {
		if(sender != null) {
			sender.send(msg);
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
	public void setEventBus(EventBus moduleEventBus) {
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
			lastTick = tick;
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
	
	@Override
	public void setMailSender(IMailSender sender) {
		this.sender = sender;
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
