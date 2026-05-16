package org.dromara.northstar.kline;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.utils.CommonUtils;

public class WeeklyBarMerger extends BarMerger{
	
	private static final LocalTime MIDNIGHT = LocalTime.of(0, 0);
	
	private LocalTime dayEndTime;
	
	private ZonedDateTime cutoffDateTime;
	
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
		
		ZonedDateTime barDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(bar.actionTimestamp()), zone());
		if(Objects.nonNull(protoBar) && !barDateTime.isAfter(cutoffDateTime)) {
			doMerge(bar);
		}
		
		if(Objects.nonNull(protoBar) && !barDateTime.isBefore(cutoffDateTime)) {
			doGenerate();
			if(!barDateTime.isAfter(cutoffDateTime)) {				
				return;
			}
		}

		if(Objects.isNull(protoBar)) {
			cutoffDateTime = cutoffDateTime(bar.actionTimestamp());
			LocalDate cutoffDate = cutoffDateTime.toLocalDate();
			LocalDate monday = cutoffDate.minusDays(cutoffDate.getDayOfWeek().getValue() - 1L);
			protoBar = Bar.builder()
					.gatewayId(bar.gatewayId())
					.contract(contract)
					.actionDay(monday)
					.actionTime(MIDNIGHT)
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(monday, MIDNIGHT)))
					.tradingDay(monday)
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
					.interval(Interval.WK1)
					.build();
		}
	
		doMerge(bar);
	}

	@Override
	public void onTick(Tick tick) {
		if(!contract.equals(tick.contract()) || tick.type() == TickType.INFO_TICK) {
			return;
		}
		if(protoBar == null) {
			cutoffDateTime = cutoffDateTime(tick.actionTimestamp());
			LocalDate cutoffDate = cutoffDateTime.toLocalDate();
			LocalDate monday = cutoffDate.minusDays(cutoffDate.getDayOfWeek().getValue() - 1L);
			protoBar = Bar.builder()
					.gatewayId(tick.gatewayId())
					.contract(contract)
					.actionDay(monday)
					.actionTime(MIDNIGHT)
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(monday, MIDNIGHT)))
					.tradingDay(monday)
					.channelType(tick.channelType())
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
					.interval(Interval.WK1)
					.build();
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
				.unstable(true)
				.build();	
        
        listeners.forEach(listener -> listener.onMergedBar(tempBar));
	}
	
	private ZoneId zone() {
		String unifiedSymbol = contract.unifiedSymbol();
		if(unifiedSymbol.endsWith("NSDQX@EQUITY") || unifiedSymbol.endsWith("FOREX")) {
			return ZoneId.of("America/New_York");
		}
		if(unifiedSymbol.contains("BINANCE")) {
			return ZoneOffset.UTC;
		}
		return ZoneOffset.systemDefault();
	}
	
	private ZonedDateTime cutoffDateTime(long t) {
		String unifiedSymbol = contract.unifiedSymbol();
		if(unifiedSymbol.endsWith("NSDQX@EQUITY")) {
			ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneId.of("America/New_York"));
			LocalDate friday = zdt.plusDays(5L - zdt.getDayOfWeek().getValue()).toLocalDate();
			return ZonedDateTime.of(LocalDateTime.of(friday, LocalTime.of(19, 59)), ZoneId.of("America/New_York"));
		}
		if(unifiedSymbol.endsWith("FOREX")) {
			ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneOffset.UTC);
			ZonedDateTime zdtFriday = zdt.plusDays(5L - zdt.getDayOfWeek().getValue()).withHour(21).withMinute(59);
			if(zdtFriday.toInstant().toEpochMilli() < t) {
				return zdtFriday.plusWeeks(1);
			}
			return zdtFriday;
		}
		if(unifiedSymbol.contains("BINANCE")) {
			ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(t), ZoneOffset.UTC);
			ZonedDateTime sunday = zdt.plusDays(7L - zdt.getDayOfWeek().getValue());
			return sunday.plusDays(1).withMinute(0).withSecond(0).withNano(0).minusSeconds(1);
		}
		LocalDate today = CommonUtils.millsToLocalDateTime(t).toLocalDate();
		LocalDate friday = today.plusDays(5L - today.getDayOfWeek().getValue());
		ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.of(friday, dayEndTime), ZoneOffset.systemDefault()) ;
		if(zdt.toInstant().toEpochMilli() < t) {
			return zdt.plusWeeks(1);
		}
		return zdt;
	}

}
