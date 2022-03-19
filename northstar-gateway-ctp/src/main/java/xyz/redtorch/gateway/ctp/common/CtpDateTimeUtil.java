package xyz.redtorch.gateway.ctp.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import tech.quantit.northstar.common.utils.MarketDateTimeUtil;

public class CtpDateTimeUtil implements MarketDateTimeUtil{

	@Override
	public LocalDate getTradingDay(LocalDateTime dateTime) {
		if(dateTime.getDayOfWeek() == DayOfWeek.FRIDAY && dateTime.toLocalTime().isAfter(LocalTime.of(16, 0))) {
			return dateTime.plusHours(56).toLocalDate();
		}
		if(dateTime.toLocalTime().isAfter(LocalTime.of(16, 0))) {
			return dateTime.plusHours(4).toLocalDate();
		}
		
		return dateTime.toLocalDate();
	}
}
