package org.dromara.northstar.common.model.core;

import java.time.LocalTime;

import lombok.Builder;

/**
 * 时间槽 
 */
@Builder
public record TimeSlot(
		/**
		 * 开始时间
		 */
		LocalTime start,
		/**
		 * 结束时间
		 */
		LocalTime end) {
}
