package org.dromara.northstar.common.utils;

import java.time.LocalDate;
import java.time.LocalTime;

public class DateTimeUtils {

	private static final LocalTime DAILY_CUTOFF = LocalTime.of(19, 0);
	
	public LocalDate convertTradingDayForCNMarket(LocalDate actionDay, LocalTime actionTime) {
		if(actionTime.isBefore(DAILY_CUTOFF) && actionDay.getDayOfWeek().getValue() < 6) {
			return actionDay;
		}
		if(actionTime.isAfter(DAILY_CUTOFF) && actionDay.getDayOfWeek().getValue() < 5) {
			return actionDay.plusDays(1);
		}
		return actionDay.plusDays(8 - actionDay.getDayOfWeek().getValue());
	}
}
