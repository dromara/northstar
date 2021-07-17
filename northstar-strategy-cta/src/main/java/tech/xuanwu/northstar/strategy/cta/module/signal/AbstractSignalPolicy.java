package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventBus;
import tech.xuanwu.northstar.strategy.common.event.ModuleEventType;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;
import tech.xuanwu.northstar.strategy.common.model.ModuleAgent;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public abstract class AbstractSignalPolicy implements SignalPolicy {
	
	/**
	 * 绑定合约
	 */
	protected String bindedUnifiedSymbol; 

	protected ModuleEventBus meb;
	
	protected Map<String, BarData> barDataMap;
	
	protected ModuleAgent agent;
	
	
	@Override
	public Set<String> bindedUnifiedSymbols() {
		return Set.of(bindedUnifiedSymbol);
	}
	
	@Override
	public void onEvent(ModuleEvent event) {
		// 信号策略属于最上流的流程，可以不用响应下游事件
	}
	
	@Override
	public void setEventBus(ModuleEventBus moduleEventBus) {
		meb = moduleEventBus;
	}


	@Override
	public void updateTick(TickField tick) {
		// 先更新行情
		if(bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {
			barDataMap.get(tick.getUnifiedSymbol()).update(tick);
			long timestamp = tick.getActionTimestamp();
			onTick((int) (timestamp % 60000));
		}
	}
	
	@Override
	public void updateBar(BarField bar) {
		if(bindedUnifiedSymbols().contains(bar.getUnifiedSymbol())) {
			barDataMap.get(bar.getUnifiedSymbol()).update(bar);
			String actionTime = bar.getActionTime();
			LocalTime time = LocalTime.parse(actionTime, DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER);
			onMin(time);
		}
		
	}
	
	/**
	 * 每TICK刷新用来驱动逻辑计算，所有的数据均已刷新在barDataMap中
	 */
	protected abstract void onTick(int millicSecOfMin);
	
	/**
	 * 每分钟刷新用来驱动逻辑计算，所有的数据均已刷新在barDataMap中
	 */
	protected abstract void onMin(LocalTime time);

	/**
	 * 子类可以直接调用这个发信号
	 * @param signal
	 */
	protected void emitSignal(CtaSignal signal) {
		log.info("信号策略生成交易信号");
		meb.post(ModuleEvent.builder()
				.eventType(ModuleEventType.SIGNAL_CREATED)
				.data(signal)
				.build());
	}

	@Override
	public BarData getRefBarData(String unifiedSymbol) {
		return barDataMap.get(unifiedSymbol);
	}


	@Override
	public void setRefBarData(Map<String, BarData> barDataMap) {
		this.barDataMap = barDataMap;
	}
	
	@Override
	public void setModuleAgent(ModuleAgent agent) {
		this.agent = agent;
	}
}
