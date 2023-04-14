package org.dromara.northstar.common.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class MarketDataLoadingUtils {
	
	private LocalTime breakingTime = LocalTime.of(20, 0);

	public LocalDate getCurrentTradeDay(long curTimestamp, boolean autoExtend) {
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(curTimestamp), ZoneId.systemDefault());
		LocalDate nowDate = ldt.toLocalDate();
		LocalTime nowTime = ldt.toLocalTime();
		if(!autoExtend || nowTime.isBefore(breakingTime) && nowDate.getDayOfWeek().getValue() < 6) {
			return nowDate;
		} else if(nowDate.getDayOfWeek().getValue() < 5) {
			return nowDate.plusDays(1);
		}
		int incrDay = 8 - nowDate.getDayOfWeek().getValue();
		return nowDate.plusDays(incrDay);
	}
	
	public LocalDate getFridayOfLastWeek(long curTimestamp) {
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(curTimestamp), ZoneId.systemDefault());
		LocalDate nowDate = ldt.toLocalDate();
		return nowDate.minusDays(nowDate.getDayOfWeek().getValue() + 2L);
	}
	
	public LocalDate getFridayOfThisWeek(LocalDate date) {
		return switch(date.getDayOfWeek()) {
		case FRIDAY -> date;
		case SATURDAY, SUNDAY -> date.minusDays(date.getDayOfWeek().getValue() - 5L);
		default -> date.plusDays(5L - date.getDayOfWeek().getValue());
		};
	}
}
