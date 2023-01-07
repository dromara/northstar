package tech.quantit.northstar.gateway.api.domain.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

public class DateUtils {

	private DateUtils() {}
	
	/**
	 * 获取N月第X个周几是哪一天
	 * @param month
	 * @param numOf
	 * @param dayOfWeek
	 * @return
	 */
	public static LocalDate numOfWeekDay(int year, Month month, long numOf, DayOfWeek dayOfWeek) {
		LocalDate firstDayOfMonth = LocalDate.of(year, month.getValue(), 1);
		if(firstDayOfMonth.getDayOfWeek().getValue() > dayOfWeek.getValue()) {
			return firstDayOfMonth.plusWeeks(numOf).minusDays(Math.abs(firstDayOfMonth.getDayOfWeek().getValue() - dayOfWeek.getValue()));
		}
		if(firstDayOfMonth.getDayOfWeek().getValue() < dayOfWeek.getValue()) {
			return firstDayOfMonth.plusWeeks(numOf - 1).plusDays(Math.abs(firstDayOfMonth.getDayOfWeek().getValue() - dayOfWeek.getValue()));
		}
		return firstDayOfMonth.plusWeeks(numOf - 1);
	}
}
