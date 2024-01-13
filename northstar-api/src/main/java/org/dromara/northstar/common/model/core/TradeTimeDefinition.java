package org.dromara.northstar.common.model.core;

import java.util.List;
import java.util.Objects;

import lombok.Builder;

@Builder
public record TradeTimeDefinition(
		List<TimeSlot> timeSlots	// 交易时段列表（必须按出现的先后顺序排列）
	) {
	
	@Override
	public int hashCode() {
		return Objects.hash(timeSlots);
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
		return Objects.equals(timeSlots, other.timeSlots);
	}
	
}
