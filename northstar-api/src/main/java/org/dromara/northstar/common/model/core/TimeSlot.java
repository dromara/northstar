package org.dromara.northstar.common.model.core;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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
	
	@Override
	public int hashCode() {
		return Objects.hash(end, start);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TimeSlot other = (TimeSlot) obj;
		return Objects.equals(end, other.end) && Objects.equals(start, other.start);
	}
	
	public boolean withinSection(LocalTime t) {
		if(start.isBefore(end)) {
			return !t.isBefore(start) && !t.isAfter(end);
		}
		return !t.isBefore(start) || !t.isAfter(end);
	}
	
	public long numOfMinute() {
		if(end.isBefore(start)) {
			return 1440 - ChronoUnit.MINUTES.between(end, start);
		}
		return ChronoUnit.MINUTES.between(start, end);
	}
}
