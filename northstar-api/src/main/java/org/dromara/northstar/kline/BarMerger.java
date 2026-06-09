package org.dromara.northstar.kline;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.dromara.northstar.common.BarDataAware;
import org.dromara.northstar.common.TickDataAware;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.strategy.MergedBarListener;

import lombok.extern.slf4j.Slf4j;

/**
 * 分钟线合成器
 * @author KevinHuangwl
 *
 * 整个Merger的合成思路是：
 * 把 TickTime, BarTime （统一标准化为）-> MinuteEntry（分钟格）然后找到每个entry对应的cutoffTime -> cutoffTime  计得-> cutoffDateTime 
 */
@Slf4j
public class BarMerger implements BarDataAware, TickDataAware {
	
	protected final Contract contract;
	
	protected long curBarTimestamp;
	
	protected LocalDateTime cutoffDateTime;
	
	protected Bar protoBar;
	
	protected final Set<MergedBarListener> listeners = new HashSet<>();
	
	protected double volDelta;
	protected int oiDelta;
	protected double turnoverDelta;
	
	protected int barVolDelta;
	protected int barOiDelta;
	protected double barTurnoverDelta;

	protected double high = Double.MIN_VALUE;
	protected double low = Double.MAX_VALUE;
	
	protected BarTimePadding barTimePadding;
	private Interval barInterval;
	
	protected LinkedList<Bar> mergingBuffer = new LinkedList<>();
	
	public BarMerger(Interval interval, Contract contract) { 
		this.contract = contract;
		this.barInterval = interval;
		this.barTimePadding = new BarTimePadding(interval, contract);
	}

	@Override
	public synchronized void onBar(Bar bar) {
		if(!contract.equals(bar.contract()) || !barTimePadding.valid(bar)) {
			if(log.isTraceEnabled()) {				
				log.trace("[{}] K线合成器忽略Bar数据 [{}]", contract.unifiedSymbol(), String.format("%s:%s:%s", bar.contract().unifiedSymbol(), bar.actionDay(), bar.actionTime()));
			}
			return;
		}
		
		LocalDateTime barEndDateTime = LocalDateTime.of(bar.actionDay(), bar.actionTime());
		if(contract.usingStartTimeInBarTime()) {
			LocalDateTime barStartDateTime = barEndDateTime;
			barEndDateTime = barStartDateTime.plusMinutes(bar.interval().getMinutes());
		}
		if(Objects.isNull(cutoffDateTime)) {			
			try {
				cutoffDateTime = barTimePadding.cutoffTimeInMinute(bar);
			} catch (IllegalStateException ex) {
				log.debug("该数据被忽略，原因：{}", ex.getMessage());
				return;
			}
		}
		
		if(bar.actionTimestamp() < curBarTimestamp) {
			if(log.isTraceEnabled()) {				
				log.trace("忽略过时数据：{} {} {} {} 当前计算时间：{}", bar.contract().name(), bar.actionDay(), bar.actionTime(), 
						bar.actionTimestamp(), LocalDateTime.ofInstant(Instant.ofEpochMilli(curBarTimestamp), ZoneId.systemDefault()));
			}
			return;
		}
		curBarTimestamp = bar.actionTimestamp();
		
		if(!barEndDateTime.isAfter(cutoffDateTime)) {
			doMerge(bar);
		}
		
		if(!barEndDateTime.isBefore(cutoffDateTime)) {
			doGenerate();
			if(barEndDateTime.isEqual(cutoffDateTime)) {	
				cutoffDateTime = null;
			} else if(barEndDateTime.isAfter(cutoffDateTime)) {
				cutoffDateTime = barTimePadding.cutoffTimeInMinute(bar);
				mergingBuffer.add(bar);
			}
		}
		
	}
	
	public synchronized void addListener(MergedBarListener listener) {
		listeners.add(listener);
	}
	
	protected void doMerge(Bar bar) {
		mergingBuffer.add(bar);
	}
	
	protected LocalDate mergedBarActionDay() {
		if(contract.usingStartTimeInBarTime()) {
			if(!mergingBuffer.isEmpty()) {
				return mergingBuffer.getFirst().actionDay();
			}
			if(barInterval == Interval.WK1) {
				return cutoffDateTime.toLocalDate().minusDays(cutoffDateTime.toLocalDate().getDayOfWeek().getValue() - 1);
			}
		}
		return cutoffDateTime.toLocalDate();
	}
	
	protected LocalTime mergedBarActionTime() {
		List<TimeSlot> times = contract.contractDefinition().tradeTimeDef().timeSlots();
		if(contract.usingStartTimeInBarTime()) {
			if(barInterval == Interval.WK1 || barInterval == Interval.D1) {
				return times.getFirst().start();
			}
			return cutoffDateTime.minusMinutes(barInterval.getMinutes()).toLocalTime();
		}
		return cutoffDateTime.toLocalTime();
	}
	
	protected void doGenerate() {
		if(mergingBuffer.isEmpty()) {
			return;
		}
		
		listeners.forEach(listener -> {
			try {
				listener.onMergedBar(aggregateBar());
			} catch (Exception e) {
				log.error("回调异常", e);
			}
		});
		mergingBuffer.clear();
		protoBar = null;
		resetVal();
	}
	
	protected Bar aggregateBar() {
		Bar firstBar = mergingBuffer.getFirst();
		return mergingBuffer.stream().reduce(Bar.builder().highPrice(Integer.MIN_VALUE).lowPrice(Integer.MAX_VALUE).build(), (accB, b) -> {
			return accB.toBuilder()
					.gatewayId(contract.gatewayId())
					.channelType(contract.channelType())
					.contract(contract)
					.tradingDay(mergedBarActionDay())
					.actionDay(mergedBarActionDay())
					.actionTime(mergedBarActionTime())
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(mergedBarActionDay(), mergedBarActionTime())))
					.openPrice(firstBar.openPrice())
					.highPrice(Math.max(accB.highPrice(), b.highPrice()))
					.lowPrice(Math.min(accB.lowPrice(), b.lowPrice()))
					.closePrice(b.closePrice())
					.volume(b.volume())
					.volumeDelta(accB.volumeDelta() + b.volumeDelta())
					.openInterest(b.openInterest())
					.openInterestDelta(accB.openInterestDelta() + b.openInterestDelta())
					.turnover(b.turnover())
					.turnoverDelta(accB.turnoverDelta() + b.turnoverDelta())
					.interval(barInterval)
					.build();
		});
	}
	
	protected void resetVal() {
		volDelta = 0;
		oiDelta = 0;
		turnoverDelta = 0;
		barVolDelta = 0;
		barOiDelta = 0;
		barTurnoverDelta = 0;
		high = Double.MIN_VALUE;
		low = Double.MAX_VALUE;
	}
	
	@Override
	public void onTick(Tick tick) {
		if(!contract.equals(tick.contract()) || tick.type() == TickType.INFO_TICK || tick.actionTimestamp() < curBarTimestamp) {
			return;
		}
		if(cutoffDateTime == null) {			
			cutoffDateTime = barTimePadding.cutoffTimeInMinute(tick);
		}
		if(protoBar == null) {
			resetVal();
			if(!mergingBuffer.isEmpty()) {
				protoBar = aggregateBar();
			} else {
				LocalDate actionDay = contract.usingStartTimeInBarTime() ? tick.actionDay() : cutoffDateTime.toLocalDate();
				LocalTime actionTime = contract.usingStartTimeInBarTime() ? tick.actionTime().withSecond(0).withNano(0) : cutoffDateTime.toLocalTime();
				protoBar = Bar.builder()
						.gatewayId(tick.gatewayId())
						.contract(contract)
						.channelType(tick.channelType())
						.tradingDay(tick.tradingDay())
						.actionDay(actionDay)
						.actionTime(actionTime)
						.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(actionDay, actionTime)))
						.openPrice(tick.lastPrice())
						.highPrice(tick.lastPrice())
						.lowPrice(tick.lastPrice())
						.closePrice(tick.lastPrice())
						.volume(tick.volume())
						.openInterest(tick.openInterest())
						.turnover(tick.turnover())
						.preClosePrice(tick.preClosePrice())
						.preOpenInterest(tick.preOpenInterest())
						.preSettlePrice(tick.preSettlePrice())
						.build();
			}
			
			oiDelta = protoBar.openInterestDelta();
			turnoverDelta = protoBar.turnoverDelta();
			volDelta = protoBar.volumeDelta();
			
			high = protoBar.highPrice();
			low = protoBar.lowPrice();
		}
		
		oiDelta += tick.openInterestDelta();
		turnoverDelta += tick.turnoverDelta();
		volDelta += tick.volumeDelta();
		
		high = Math.max(high, tick.lastPrice());
		low = Math.min(low, tick.lastPrice());
		
		Bar tempBar = protoBar.toBuilder()
				.closePrice(tick.lastPrice())
				.highPrice(high)
				.lowPrice(low)
				.volume(tick.volume())
				.volumeDelta(volDelta)
				.openInterest(tick.openInterest())
				.openInterestDelta(oiDelta)
				.turnover(tick.turnover())
				.turnoverDelta(turnoverDelta)
				.interval(barInterval)
				.unstable(true)
				.build();	
        
        listeners.forEach(listener -> listener.onMergedBar(tempBar));
        
	}
	
	public Bar getCurrent() {
		return protoBar;
	}
	
}
