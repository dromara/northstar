package tech.quantit.northstar.gateway.playback.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import tech.quantit.northstar.common.IHolidayManager;

/**
 * CTP时钟
 * @author KevinHuangwl
 *
 */
public class CtpPlaybackClock implements PlaybackClock {
	
	private IHolidayManager holidayMgr;
	
	private LocalDateTime ldt;
	
	private int t1Start = LocalTime.of(0, 0).toSecondOfDay();
	private int t1End = LocalTime.of(2, 30).toSecondOfDay();
	private int t2Start = LocalTime.of(9, 0).toSecondOfDay();
	private int t2End = LocalTime.of(11, 30).toSecondOfDay();
	private int t3Start = LocalTime.of(13, 30).toSecondOfDay();
	private int t3End = LocalTime.of(15, 15).toSecondOfDay();
	private int t4Start = LocalTime.of(21, 0).toSecondOfDay();
	private int t4End = LocalTime.of(23, 59, 59).toSecondOfDay();
	
	public CtpPlaybackClock(IHolidayManager holidayMgr, long initTimestamp) {
		this.holidayMgr = holidayMgr;
		this.ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(initTimestamp), ZoneId.systemDefault());
	}

	@Override
	public long nextMarketMinute() {
		ldt = ldt.plusMinutes(1);
		while(!withinSection(ldt)) {
			ldt = ldt.plusMinutes(15);
		}
		return ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
	}

	private boolean withinSection(LocalDateTime ldt) {
		int secondOfDay = ldt.toLocalTime().toSecondOfDay();
		if(holidayMgr.isHoliday(ldt)) {
			return false;
		}
		return secondOfDay >= t1Start && secondOfDay < t1End
			|| secondOfDay >= t2Start && secondOfDay < t2End
			|| secondOfDay >= t3Start && secondOfDay < t3End
			|| secondOfDay >= t4Start && secondOfDay < t4End;
	} 
}
