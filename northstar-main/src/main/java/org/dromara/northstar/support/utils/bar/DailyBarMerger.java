package org.dromara.northstar.support.utils.bar;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.TimeSlot;

/**
 * 日线合成器
 * @author KevinHuangwl
 *
 */
public class DailyBarMerger extends BarMerger{

	private final int numOfDayPerBar;
	
	private final LocalTime dayEndTime;
	
	private AtomicInteger count;
	
	public DailyBarMerger(int numOfDayPerBar, Contract contract) {
		super(0, contract);
		this.numOfDayPerBar = numOfDayPerBar;
		List<TimeSlot> times = contract.contractDefinition().tradeTimeDef().timeSlots();
		this.dayEndTime = times.get(times.size() - 1).end();
	}

	@Override
	public synchronized void onBar(Bar bar) {
		if(!contract.equals(bar.contract())) {
			return;
		}

		if(Objects.isNull(protoBar)) {
			count = new AtomicInteger(0);
			protoBar = Bar.builder()
					.gatewayId(bar.gatewayId())
					.contract(contract)
					.actionDay(bar.actionDay())
					.actionTime(bar.actionTime())
					.actionTimestamp(bar.actionTimestamp())
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
		}

		if(bar.actionTime().equals(dayEndTime) && count.incrementAndGet() == numOfDayPerBar) {
			doGenerate();
			return;
		}
		
		doMerge(bar);
	}

	
}
