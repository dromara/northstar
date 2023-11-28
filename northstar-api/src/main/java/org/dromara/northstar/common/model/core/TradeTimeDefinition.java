package org.dromara.northstar.common.model.core;

import java.util.List;

import lombok.Builder;

@Builder
public record TradeTimeDefinition(
		List<TimeSlot> timeSlots
	) {

}
