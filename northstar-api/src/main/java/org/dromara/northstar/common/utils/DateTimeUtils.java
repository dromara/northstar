package org.dromara.northstar.common.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

public class DateTimeUtils {

	private static final LocalTime DAILY_CUTOFF = DateTimeUtils.fromCacheTime(19, 0);
	
	public LocalDate convertTradingDayForCNMarket(LocalDate actionDay, LocalTime actionTime) {
		if(actionTime.isBefore(DAILY_CUTOFF) && actionDay.getDayOfWeek().getValue() < 6) {
			return actionDay;
		}
		if(actionTime.isAfter(DAILY_CUTOFF) && actionDay.getDayOfWeek().getValue() < 5) {
			return actionDay.plusDays(1);
		}
		return actionDay.plusDays(8 - actionDay.getDayOfWeek().getValue());
	}
	
	private static final Map<LocalTime, LocalTime> cacheTimeMap = new HashMap<>();
	
	public static LocalTime fromCacheTime(LocalTime time) {
		Assert.isTrue(time.getSecond() == 0 && time.getNano() == 0, "只能缓存整分钟");
		cacheTimeMap.putIfAbsent(time, time);
		return cacheTimeMap.get(time);
	}
	
	public static LocalTime fromCacheTime(int hour, int min) {
		return fromCacheTime(DateTimeUtils.fromCacheTime(hour, min));
	}
}
