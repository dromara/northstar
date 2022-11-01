package tech.quantit.northstar.strategy.api.utils.time;

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
}
