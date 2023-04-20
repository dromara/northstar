package org.dromara.northstar.gateway.model;

import java.time.LocalTime;

public class PeriodSegment {
	
	private LocalTime start;
	private LocalTime end;

	public PeriodSegment(LocalTime start, LocalTime end) {
		this.start = start;
		this.end = end;
	}
	
	public boolean withinPeriod(LocalTime t) {
		if(end.isAfter(start)) {			
			return !t.isBefore(start) && !t.isAfter(end);
		}
		return !t.isBefore(start) || !t.isAfter(end);
	}
	
	public LocalTime endOfSegment() {
		return end;
	}
	
	public LocalTime startOfSegment() {
		return start;
	}
}
