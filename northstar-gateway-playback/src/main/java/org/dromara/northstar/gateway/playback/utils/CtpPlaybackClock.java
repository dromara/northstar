package org.dromara.northstar.gateway.playback.utils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * CTP时钟
 * @author KevinHuangwl
 *
 */
@Deprecated(forRemoval = true)
public class CtpPlaybackClock implements PlaybackClock {
	
	private LocalDateTime ldt;
	
	private int t1Start = LocalTime.of(0, 0).toSecondOfDay();
	private int t1End = LocalTime.of(2, 30).toSecondOfDay();
	private int t2Start = LocalTime.of(9, 0).toSecondOfDay();
	private int t2End = LocalTime.of(11, 30).toSecondOfDay();
	private int t3Start = LocalTime.of(13, 30).toSecondOfDay();
	private int t3End = LocalTime.of(15, 15).toSecondOfDay();
	private int t4Start = LocalTime.of(21, 0).toSecondOfDay();
	private int t4End = LocalTime.of(23, 59, 59).toSecondOfDay();
	
	public CtpPlaybackClock(LocalDateTime ldt) {
		this.ldt = ldt;
	}

	@Override
	public LocalDateTime nextMarketMinute() {
		ldt = ldt.plusMinutes(1);
		while(!withinSection(ldt)) {
			ldt = ldt.plusMinutes(15);
		}
		return ldt;
	}

	private boolean withinSection(LocalDateTime ldt) {
		int secondOfDay = ldt.toLocalTime().toSecondOfDay();
		if(ldt.getDayOfWeek() == DayOfWeek.SUNDAY 
				|| ldt.getDayOfWeek() == DayOfWeek.SATURDAY && secondOfDay > t1End
				|| ldt.getDayOfWeek() == DayOfWeek.MONDAY && secondOfDay < t1End) {
			return false;
		}
		return secondOfDay >= t1Start && secondOfDay < t1End
			|| secondOfDay >= t2Start && secondOfDay < t2End
			|| secondOfDay >= t3Start && secondOfDay < t3End
			|| secondOfDay >= t4Start && secondOfDay < t4End;
	} 
}
