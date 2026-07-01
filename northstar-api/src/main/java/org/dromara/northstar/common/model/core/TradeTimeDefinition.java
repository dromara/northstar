package org.dromara.northstar.common.model.core;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import lombok.Builder;

@Builder
public record TradeTimeDefinition(
		List<TimeSlot> timeSlots,	// 交易时段列表（必须按出现的先后顺序排列）
		Boolean weekendClosure		// 周末休市
	) {
	
	public TradeTimeDefinition {
		if(weekendClosure == null) {
			weekendClosure = true;
		}
	}
	
	public boolean withinTradeTime(LocalTime t) {
		for(TimeSlot ts : timeSlots) {
			if(ts.withinSection(t)) {
				return true;
			}
		}
		return false;
	}
	
	private static final int SEC_PER_MIN = 60;
	private static final int MINS_OF_DAY = 1440;
	
	public LocalTime nearestTradeTime(LocalTime t) {
		LocalTime base = t.withSecond(0).withNano(0);
		int minuteIdx = base.toSecondOfDay() / SEC_PER_MIN;
		
		for (int offset = 0; offset < MINS_OF_DAY; offset++) {
			int forwardIdx = (minuteIdx + offset) % MINS_OF_DAY;
			LocalTime forwardTime = LocalTime.ofSecondOfDay(forwardIdx * SEC_PER_MIN);
			if (withinTradeTime(forwardTime)) {
				return forwardTime;
			}
			if (offset > 0) {
				int backwardIdx = (minuteIdx - offset + MINS_OF_DAY) % MINS_OF_DAY;
				LocalTime backwardTime = LocalTime.ofSecondOfDay(backwardIdx * SEC_PER_MIN);
				if (withinTradeTime(backwardTime)) {
					return backwardTime;
				}
			}
		}
		return base;
	}

	@Override
	public int hashCode() {
		return Objects.hash(timeSlots, weekendClosure);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TradeTimeDefinition other = (TradeTimeDefinition) obj;
		return Objects.equals(timeSlots, other.timeSlots) && Objects.equals(weekendClosure, other.weekendClosure);
	}
	
}
