package org.dromara.northstar.support.utils.bar;

import static xyz.redtorch.pb.CoreEnum.BarCycleEnum.B_1Day;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.utils.CommonUtils;

import xyz.redtorch.pb.CoreEnum.BarCycleEnum;

/**
 * 日线合成器
 * @author KevinHuangwl
 *
 */
public class DailyBarMerger extends BarMerger{

	private LocalDate tradingDay;
	
	private LocalTime dayEndTime;
	
	public DailyBarMerger(Contract contract) {
		super(0, contract);
		this.barPeriod = B_1Day;
		List<TimeSlot> times = contract.contractDefinition().tradeTimeDef().timeSlots();
		this.dayEndTime = times.get(times.size() - 1).end();
	}

	@Override
	public synchronized void onBar(Bar bar) {
		if(!contract.equals(bar.contract()) || bar.barPeriod() == BarCycleEnum.B_1Wk) {
			return;
		}
		
		if(bar.barPeriod() == BarCycleEnum.B_1Day) {
			Bar b = bar.toBuilder()
					.actionTime(dayEndTime)
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(bar.tradingDay(), dayEndTime)))
					.build();
			listeners.forEach(listener -> listener.onMergedBar(b));
			return;
		}
		
		if(Objects.nonNull(protoBar) && !bar.tradingDay().isAfter(tradingDay)) {
			doMerge(bar);
		}
		
		if(Objects.nonNull(protoBar) && (bar.tradingDay().equals(tradingDay) && bar.actionTime().equals(dayEndTime)
				|| bar.tradingDay().isAfter(tradingDay))) {
			doGenerate();
			if(!bar.tradingDay().isAfter(tradingDay)) {
				return;
			}
		}

		if(Objects.isNull(protoBar)) {
			tradingDay = bar.tradingDay();
			protoBar = Bar.builder()
					.gatewayId(bar.gatewayId())
					.contract(contract)
					.actionDay(bar.tradingDay())
					.actionTime(dayEndTime)
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(tradingDay, dayEndTime)))
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
					.barPeriod(barPeriod)
					.build();
			
			if(bar.tradingDay().equals(tradingDay) && bar.actionTime().equals(dayEndTime)) {				
				doGenerate();
			}
		}
	
	}

	@Override
	public void onTick(Tick tick) {
		if(!contract.equals(tick.contract()) || tick.type() == TickType.INFO_TICK) {
			return;
		}
		if(protoBar == null) {
			tradingDay = tick.tradingDay();
			protoBar = Bar.builder()
					.gatewayId(tick.gatewayId())
					.contract(contract)
					.actionDay(tradingDay)
					.actionTime(dayEndTime)
					.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(tradingDay, dayEndTime)))
					.tradingDay(tick.tradingDay())
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
					.barPeriod(barPeriod)
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
	
}
