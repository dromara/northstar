package tech.xuanwu.northstar.strategy.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.api.constant.ModuleState;
import tech.xuanwu.northstar.strategy.api.constant.Signal;
import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.api.model.TimeSeriesData;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public abstract class AbstractSignalPolicy implements SignalPolicy {

	protected ModuleEventBus moduleEventBus;
	
	protected ModuleState currentState;
	
	protected String bindedUnifiedSymbol;
	
	protected List<MarketDataReceiver> mdrList = new ArrayList<>();
	
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
	
	
	protected void registerDataReceiver(MarketDataReceiver marketDataReceiver) {
		moduleEventBus.register(marketDataReceiver);
		mdrList.add(marketDataReceiver);
	}

	@Override
	public List<TimeSeriesData> inspectRefData() {
		if(mdrList.isEmpty()) 
			return Collections.emptyList();
		return mdrList.stream().map(MarketDataReceiver::inspection).collect(Collectors.toList());
	}
	
	@Override
	public void initByTick(Iterable<TickField> ticks) {
		for(MarketDataReceiver mdr : mdrList) {
			mdr.initByTick(ticks);
		}
	}

	@Override
	public void initByBar(Iterable<BarField> bars) {
		for(MarketDataReceiver mdr : mdrList) {
			mdr.initByBar(bars);
		}
	}
	
}
