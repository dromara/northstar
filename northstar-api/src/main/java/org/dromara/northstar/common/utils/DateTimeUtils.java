package org.dromara.northstar.common.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

public class DateTimeUtils {

	private static final Map<LocalTime, LocalTime> cacheTimeMap = new HashMap<>();
	private static final LocalTime DAILY_CUTOFF = DateTimeUtils.fromCacheTime(19, 0);
	
	public LocalDate convertTradingDayForCNMarket(LocalDate actionDay, LocalTime actionTime) {
		if(actionTime.isBefore(DAILY_CUTOFF) && actionDay.getDayOfWeek().getValue() < 6) {
			return actionDay;
		}
		if(actionTime.isAfter(DAILY_CUTOFF) && actionDay.getDayOfWeek().getValue() < 5) {
			return actionDay.plusDays(1);
		}
		return actionDay.plusDays(8L - actionDay.getDayOfWeek().getValue());
	}
	
	public static int weekOfYear(LocalDate date) {
		return date.get(WeekFields.ISO.weekBasedYear()) * 100 + date.get(WeekFields.ISO.weekOfWeekBasedYear());
	}
	
	public static LocalDate fromWeekOfYear(int weekOfYear, DayOfWeek weekday) {
		int year = weekOfYear / 100;
		int week = weekOfYear % 100;
		return LocalDate.of(year, 1, 1).plusWeeks(week - 1L)
				.with(WeekFields.ISO.dayOfWeek(), weekday.getValue());
	}
	
	public static synchronized LocalTime fromCacheTime(LocalTime time) {
		Assert.isTrue(time.getSecond() == 0 && time.getNano() == 0, "只能缓存整分钟");
		cacheTimeMap.putIfAbsent(time, time);
		return cacheTimeMap.get(time);
	}
	
	public static synchronized LocalTime fromCacheTime(int hour, int min) {
		return fromCacheTime(LocalTime.of(hour, min));
	}
}
