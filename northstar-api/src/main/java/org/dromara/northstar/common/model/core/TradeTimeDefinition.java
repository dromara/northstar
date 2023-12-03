package org.dromara.northstar.common.model.core;

import java.util.List;

import lombok.Builder;

@Builder
public record TradeTimeDefinition(
		List<TimeSlot> timeSlots	// 交易时段列表（必须按出现的先后顺序排列）
	) {

}
