package org.dromara.northstar.kline;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.DateTimeUtils;

public class WeeklyBarMerger extends BarMerger{
	
	private LocalTime dayEndTime;
	
	public WeeklyBarMerger(Contract contract) {
		super(Interval.WK1, contract);
		List<TimeSlot> times = contract.contractDefinition().tradeTimeDef().timeSlots();
		this.dayEndTime = times.getLast().end();
	}
	
	@Override
	public synchronized void onBar(Bar bar) {
		if(!contract.equals(bar.contract())) {
			return;
		}
		
		if(bar.interval() == Interval.WK1) {
			listeners.forEach(listener -> listener.onMergedBar(bar));
			return;
		}
		
		if(cutoffDateTime == null) {
			cutoffDateTime = cutoff(bar.tradingDay());
		}
		
		if(!CommonUtils.millsToLocalDateTime(bar.actionTimestamp()).isAfter(cutoffDateTime)) {
			doMerge(bar);
		}
		
		if(!CommonUtils.millsToLocalDateTime(bar.actionTimestamp()).isBefore(cutoffDateTime)) {
			doGenerate();
			if(CommonUtils.millsToLocalDateTime(bar.actionTimestamp()).isAfter(cutoffDateTime)) {				
				cutoffDateTime = cutoff(bar.tradingDay());
				doMerge(bar);
			} else {
				cutoffDateTime = null;
			}
		}

	}
	
	private LocalDateTime cutoff(LocalDate tradingDay) {
		int currentBarWeekOfYear = DateTimeUtils.weekOfYear(tradingDay);
		DayOfWeek cutoffDay = contract.activeInWeekend() ? DayOfWeek.SUNDAY : DayOfWeek.FRIDAY;
		return LocalDateTime.of(DateTimeUtils.fromWeekOfYear(currentBarWeekOfYear, cutoffDay), dayEndTime);
	}
	
	@Override
	public void onTick(Tick tick) {
		if(!contract.equals(tick.contract()) || tick.type() == TickType.INFO_TICK) {
			return;
		}
		
		if(cutoffDateTime == null) {
			cutoffDateTime = cutoff(tick.tradingDay());
		}
		
		if(protoBar == null) {
			resetVal();
			if(!mergingBuffer.isEmpty()) {
				protoBar = aggregateBar();
			} else {
				protoBar = Bar.builder()
						.gatewayId(tick.gatewayId())
						.channelType(tick.channelType())
						.contract(contract)
						.tradingDay(mergedBarActionDay())
						.actionDay(mergedBarActionDay())
						.actionTime(mergedBarActionTime())
						.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(mergedBarActionDay(), mergedBarActionTime())))
						.openPrice(tick.openPrice() == 0 ? tick.lastPrice() : tick.openPrice())
						.highPrice(tick.highPrice())
						.lowPrice(tick.lowPrice())
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
				.interval(Interval.WK1)
				.unstable(true)
				.build();	
        
        listeners.forEach(listener -> listener.onMergedBar(tempBar));
	}
	
}
