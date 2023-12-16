package org.dromara.northstar.support.utils.bar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.DateTimeUtils;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.MergedBarListener;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry.ListenerType;

import lombok.extern.slf4j.Slf4j;

/**
 * 分钟线合成器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class BarMerger implements BarDataAware{
	
	protected final Contract contract;
	
	protected long curBarTimestamp;
	
	private final int numOfMinPerBar;
	
	private LocalDateTime cutoffDateTime;
	
	protected Bar protoBar;
	
	protected final Map<ListenerType, Set<MergedBarListener>> listenerMap = new EnumMap<>(ListenerType.class);
	
	/**
	 * 把分钟时间对齐，并保存一份映射表，以快速得到任意时刻的K线结算时间
	 */
	protected final Map<LocalTime, LocalTime> barTimeMap;
	
	public BarMerger(int numOfMinPerBar, Contract contract) { 
		this.contract = contract;
		this.numOfMinPerBar = numOfMinPerBar;
		this.barTimeMap = genBarTimeMap();
		listenerMap.put(ListenerType.INDICATOR, new HashSet<>());
		listenerMap.put(ListenerType.CONTEXT, new HashSet<>());
		listenerMap.put(ListenerType.STRATEGY, new HashSet<>());
	}

	// 该方法用于生成分钟线时间对齐的映射表
	// 先从合约定义中获取交易时间段，然后每分钟取整点分钟时间，作为key，对应的K线结算时间作为value
	// 例如：交易时间段为 09:00-11:30, 13:30-15:00
	// 假设numOfMinPerBar为5，则生成的映射表为：09:00 -> 09:05, 09:01 -> 09:05, 09:02 -> 09:05, 09:03 -> 09:05, 09:04 -> 09:05, 09:05 -> 09:05, 09:06 -> 09:10... 
	private Map<LocalTime, LocalTime> genBarTimeMap() {
		Map<LocalTime, LocalTime> tMap = new LinkedHashMap<>();
		List<LocalTime> valueTimeList = new ArrayList<>();
		LocalDate dummyDate = LocalDate.now();	// 引入日期解决跨日计算问题
		LocalDateTime ldtStart = null;
		LocalDateTime ldtEnd = null;
		int count = 0;
		// 先根据交易时间段生成K线时间点列表
		for(TimeSlot tslot : contract.contractDefinition().tradeTimeDef().timeSlots()) {
			boolean overnight = !tslot.end().isAfter(tslot.start()); 
			ldtStart = LocalDateTime.of(dummyDate, tslot.start());
			ldtEnd = LocalDateTime.of(overnight ? dummyDate.plusDays(1) : dummyDate, tslot.end());
			LocalDateTime ldt = ldtStart;
			while(!ldt.isAfter(ldtEnd)) {
				if(count == numOfMinPerBar || count > 0 && ldt.toLocalTime().equals(endOfTradeDateTime())) {					
					valueTimeList.add(DateTimeUtils.fromCacheTime(ldt.toLocalTime()));
					count = 0;
				}
				ldt = ldt.plusMinutes(1);
				if(!ldt.isAfter(ldtEnd)) {					
					count++;
				}
			}
		}
		// 再一次循环，生成映射表
		int i = 0;
		LocalDateTime preldt = null;
		for(TimeSlot tslot : contract.contractDefinition().tradeTimeDef().timeSlots()) {
			boolean overnight = !tslot.end().isAfter(tslot.start()); 
			ldtStart = LocalDateTime.of(dummyDate, tslot.start());
			ldtEnd = LocalDateTime.of(overnight ? dummyDate.plusDays(1) : dummyDate, tslot.end());
			LocalDateTime ldt = ldtStart;
			while(!ldt.isAfter(ldtEnd)) {
				LocalTime barTime = valueTimeList.get(i);
				LocalDateTime barDateTime = LocalDateTime.of(ldt.toLocalDate(), barTime);
				if(Objects.nonNull(preldt) && preldt.isAfter(barDateTime)) {
					barDateTime = LocalDateTime.of(ldtEnd.toLocalDate(), barTime);
				}
				if(!ldt.isAfter(barDateTime)) {
					preldt = barDateTime;
					LocalTime t = DateTimeUtils.fromCacheTime(ldt.toLocalTime());
					tMap.put(t, barTime);
					if(t.equals(barTime)) {
						i++;
					}
				}
				ldt = ldt.plusMinutes(1);
			}
		}
		return tMap;
	}

	private LocalTime endOfTradeDateTime() {
		List<TimeSlot> timeSlots = contract.contractDefinition().tradeTimeDef().timeSlots();
		return timeSlots.get(timeSlots.size() - 1).end();
	}

	public synchronized void addListener(MergedBarListener listener) {
		ListenerType type = ListenerType.INDICATOR;
		if(listener instanceof IModuleContext) {
			type = ListenerType.CONTEXT;
		} else if(listener instanceof TradeStrategy) {
			type = ListenerType.STRATEGY;
		}
		listenerMap.get(type).add(listener);
	}
	
	protected LocalDateTime toCutoffDateTime(Bar bar) {
		LocalTime keyTime = bar.actionTime().withSecond(0).withNano(0);
		if(!barTimeMap.containsKey(keyTime)) {
			throw new IllegalStateException(String.format("[%s] 没有对应的K线时间", bar.actionTime()));
		}
		LocalTime cutoffTime = barTimeMap.get(keyTime);
		if(cutoffTime.isBefore(bar.actionTime())) {
			return LocalDateTime.of(bar.actionDay().plusDays(1), cutoffTime);
		}
		return LocalDateTime.of(bar.actionDay(), cutoffTime);
	}
	
	@Override
	public synchronized void onBar(Bar bar) {
		if(!contract.equals(bar.contract()) || !barTimeMap.containsKey(bar.actionTime())) {
			if(log.isTraceEnabled()) {				
				log.trace("[{}] K线合成器忽略Bar数据 [{}]", contract.unifiedSymbol(), String.format("%s:%s:%s", bar.contract().unifiedSymbol(), bar.actionDay(), bar.actionTime()));
			}
			return;
		}
		LocalDateTime barDateTime = LocalDateTime.of(bar.actionDay(), bar.actionTime());
		if(Objects.isNull(cutoffDateTime)) {
			cutoffDateTime = toCutoffDateTime(bar);
		}
		
		if(bar.actionTimestamp() < curBarTimestamp) {
			if(log.isTraceEnabled()) {				
				log.trace("当前计算时间：{}", LocalDateTime.ofInstant(Instant.ofEpochMilli(curBarTimestamp), ZoneId.systemDefault()));
				log.trace("忽略过时数据：{} {} {} {}", bar.contract().unifiedSymbol(), bar.actionDay(), bar.actionTime(), bar.actionTimestamp());
			}
			return;
		}
		curBarTimestamp = bar.actionTimestamp();
		
		if(Objects.nonNull(protoBar) && !barDateTime.isAfter(cutoffDateTime)) {
			doMerge(bar);
		}
		
		if(Objects.nonNull(protoBar) && !barDateTime.isBefore(cutoffDateTime)) {
			doGenerate();
			return;
		}
		
		if(Objects.isNull(protoBar)) {
			cutoffDateTime = toCutoffDateTime(bar);
			protoBar = Bar.builder()
					.gatewayId(bar.gatewayId())
					.contract(contract)
					.actionDay(cutoffDateTime.toLocalDate())
					.actionTime(cutoffDateTime.toLocalTime())
					.actionTimestamp(CommonUtils.localDateTimeToMills(cutoffDateTime))
					.tradingDay(bar.tradingDay())
					.channelType(bar.channelType())
					.openPrice(bar.openPrice())
					.highPrice(bar.highPrice())
					.lowPrice(bar.lowPrice())
					.closePrice(bar.closePrice())
					.volumeDelta(bar.volumeDelta())
					.openInterestDelta(bar.openInterestDelta())
					.turnoverDelta(bar.turnoverDelta())
					.preClosePrice(bar.preClosePrice())
					.preOpenInterest(bar.preOpenInterest())
					.preSettlePrice(bar.preSettlePrice())
					.build();
			
			if(barDateTime.isEqual(cutoffDateTime)){
				doGenerate();
			}
		}
	}
	
	protected void doGenerate() {
		listenerMap.get(ListenerType.INDICATOR).forEach(listener -> listener.onMergedBar(protoBar));
		listenerMap.get(ListenerType.STRATEGY).forEach(listener -> listener.onMergedBar(protoBar));
		listenerMap.get(ListenerType.CONTEXT).forEach(listener -> listener.onMergedBar(protoBar));
		protoBar = null;
	}
	
	protected void doMerge(Bar bar) {
		double high = protoBar.highPrice();
		double low = protoBar.lowPrice();
		long volumeDelta = protoBar.volumeDelta();
		double turnoverDelta = protoBar.turnoverDelta();
		double openInterestDelta = protoBar.openInterestDelta();
		
        protoBar = protoBar.toBuilder()
        		.actionDay(bar.actionDay())
        		.actionTime(bar.actionTime())
        		.actionTimestamp(bar.actionTimestamp())
				.closePrice(bar.closePrice())
				.highPrice(Math.max(high, bar.highPrice()))
				.lowPrice(Math.min(low, bar.lowPrice()))
				.volume(bar.volume())
				.volumeDelta(volumeDelta + bar.volumeDelta())
				.openInterest(bar.openInterest())
				.openInterestDelta(openInterestDelta + bar.openInterestDelta())
				.turnover(bar.turnover())
				.turnoverDelta(turnoverDelta + bar.turnoverDelta())
				.build();		
	}
	
}
