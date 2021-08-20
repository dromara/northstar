package tech.xuanwu.northstar.strategy.cta.module.signal;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import tech.xuanwu.northstar.strategy.common.Signal;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;
import tech.xuanwu.northstar.strategy.common.constants.SignalOperation;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;
import tech.xuanwu.northstar.strategy.common.model.data.BarData;
import tech.xuanwu.northstar.strategy.common.model.state.ModuleStateMachine;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public abstract class AbstractSignalPolicy implements SignalPolicy {
	
	/**
	 * 绑定合约
	 */
	protected String bindedUnifiedSymbol; 

	protected Map<String, BarData> barDataMap;
	
	protected ModuleStateMachine stateMachine;
	
	
	@Override
	public Set<String> bindedUnifiedSymbols() {
		if(StringUtils.isEmpty(bindedUnifiedSymbol)) {
			return Set.of();
		}
		return Set.of(bindedUnifiedSymbol);
	}
	
	@Override
	public Optional<Signal> updateTick(TickField tick) {
		// 先更新行情
		if(bindedUnifiedSymbols().contains(tick.getUnifiedSymbol())) {
			BarData barData = barDataMap.get(tick.getUnifiedSymbol());
			long timestamp = tick.getActionTimestamp();
			// 整分钟时不触发，避免onTick与onMin逻辑重复，导致重复触发
			int secondOfMin = (int) (timestamp % 60000);
			if(secondOfMin == 0) {
				return Optional.empty();
			}
			Optional<Signal> result = onTick(secondOfMin, barData);
			if(result == null) {
				return Optional.empty();
			}
			return result;
		}
		return Optional.empty();
	}
	
	@Override
	public Optional<Signal> updateBar(BarField bar) {
		// 先更新行情
		if(bindedUnifiedSymbols().contains(bar.getUnifiedSymbol())) {
			BarData barData = barDataMap.get(bar.getUnifiedSymbol());
			String actionTime = bar.getActionTime();
			LocalTime time = LocalTime.parse(actionTime, DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER);
			Optional<Signal> result = onMin(time, barData);
			if(result == null) {
				return Optional.empty();
			}
			return result;
		}
		return Optional.empty();
	}
	
	/**
	 * 每TICK刷新用来驱动逻辑计算，所有的数据均已刷新在barDataMap中
	 */
	protected Optional<Signal> onTick(int milliSecOfMin, BarData barData){
		return Optional.empty();
	}
	
	/**
	 * 每分钟刷新用来驱动逻辑计算，所有的数据均已刷新在barDataMap中
	 */
	protected Optional<Signal> onMin(LocalTime time, BarData barData){
		return Optional.empty();
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
	public void setStateMachine(ModuleStateMachine stateMachine) {
		this.stateMachine = stateMachine;
	}
	
	protected Signal genSignal(SignalOperation signalOperation, double price, double stopPrice) {
		return CtaSignal.builder()
			.id(UUID.randomUUID())
			.signalClass(this.getClass())
			.signalPrice(price)
			.state(SignalOperation.SellClose)
			.sourceUnifiedSymbol(bindedUnifiedSymbol)
			.timestamp(System.currentTimeMillis())
			.build();
	}
	
	protected Signal genSignal(SignalOperation signalOperation, double price) {
		return genSignal(signalOperation, price, 0);
	}
}
