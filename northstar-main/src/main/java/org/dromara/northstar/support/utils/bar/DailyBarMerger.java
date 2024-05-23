package org.dromara.northstar.support.utils.bar;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.utils.CommonUtils;

/**
 * 日线合成器
 * @author KevinHuangwl
 *
 */
public class DailyBarMerger extends BarMerger{

	private final LocalTime dayEndTime;
	
	private long cutoffTime;
	
	public DailyBarMerger(int numOfDayPerBar, Contract contract) {
		super(0, contract);
		List<TimeSlot> times = contract.contractDefinition().tradeTimeDef().timeSlots();
		this.dayEndTime = times.get(times.size() - 1).end();
	}

	@Override
	public synchronized void onBar(Bar bar) {
		if(!contract.equals(bar.contract())) {
			return;
		}

		if(Objects.isNull(protoBar)) {
			cutoffTime = CommonUtils.localDateTimeToMills(LocalDateTime.of(bar.tradingDay(), dayEndTime));
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
	
		if(bar.getTimestamp() <= cutoffTime) {			
			doMerge(bar);
		}

		if(bar.getTimestamp() >= cutoffTime) {
			doGenerate();
		}
	}
	
}
