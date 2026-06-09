package org.dromara.northstar.kline;

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

import lombok.extern.slf4j.Slf4j;

/**
 * 日线合成器
 * @author KevinHuangwl
 *
 */
@Slf4j
public class DailyBarMerger extends BarMerger{

	private LocalDate tradingDay;
	
	private LocalTime dayEndTime;
	
	private LocalTime dayStartTime;
	
	public DailyBarMerger(Contract contract) {
		super(Interval.D1, contract);
		List<TimeSlot> times = contract.contractDefinition().tradeTimeDef().timeSlots();
		this.dayEndTime = times.getLast().end();
		this.dayStartTime = times.getFirst().start();
	}
	
	private LocalTime barTime() {
		return contract.usingStartTimeInBarTime() ? dayStartTime : dayEndTime;
	}
	
	@Override
	public synchronized void onBar(Bar bar) {
		if(!contract.equals(bar.contract()) || bar.interval() == Interval.WK1) {
			return;
		}
		
		if(bar.interval() == Interval.D1) {
			listeners.forEach(listener -> listener.onMergedBar(bar));
			return;
		}
		
		if(cutoffDateTime == null) {
			cutoffDateTime = LocalDateTime.of(bar.tradingDay(), dayEndTime);
		}
		
		if(tradingDay == null) {
			tradingDay = bar.tradingDay();
		}
		
		if(bar.tradingDay().isEqual(tradingDay)) {
			doMerge(bar);
		}
		
		if((bar.tradingDay().equals(tradingDay) && bar.actionTime().equals(dayEndTime)
				|| !CommonUtils.millsToLocalDateTime(bar.actionTimestamp()).isBefore(cutoffDateTime))) {
			doGenerate();
			if(bar.tradingDay().isAfter(tradingDay)) {
				tradingDay = bar.tradingDay();
				cutoffDateTime = LocalDateTime.of(tradingDay, dayEndTime);
				doMerge(bar);
			} else {
				tradingDay = null;
				cutoffDateTime = null;
			}
		}
	
	}

	@Override
	public void onTick(Tick tick) {
		if(!contract.equals(tick.contract()) || tick.type() == TickType.INFO_TICK) {
			return;
		}
		if(protoBar == null) {
			resetVal();
			tradingDay = tick.tradingDay();
			if(!mergingBuffer.isEmpty()) {
				protoBar = aggregateBar();
			} else {
				protoBar = Bar.builder()
						.gatewayId(contract.gatewayId())
						.channelType(contract.channelType())
						.contract(contract)
						.tradingDay(tick.tradingDay())
						.actionDay(tradingDay)
						.actionTime(barTime())
						.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(tradingDay, barTime())))
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
				.interval(Interval.D1)
				.unstable(true)
				.build();	
        
        listeners.forEach(listener -> listener.onMergedBar(tempBar));
	}
	
}
