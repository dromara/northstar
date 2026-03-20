package org.dromara.northstar.gateway.utils;

import java.time.LocalDate;
import java.util.function.BiConsumer;

public class DataLoadUtil {

	public void splitByWeek(LocalDate startDate, LocalDate endDate, BiConsumer<LocalDate, LocalDate> queryExecutor) {
		LocalDate date = startDate;
		while(!date.isAfter(endDate)) {
			LocalDate start = date;
			LocalDate endOfThisWeek = date.plusDays(7L - date.getDayOfWeek().getValue());
			LocalDate end = endDate.isBefore(endOfThisWeek) ? endDate : endOfThisWeek;
			queryExecutor.accept(start, end);
			date = end.plusDays(1);
		}
	}
	
	public void splitByMonth(LocalDate startDate, LocalDate endDate, BiConsumer<LocalDate, LocalDate> queryExecutor) {
		LocalDate date = startDate;
		while(!date.isAfter(endDate)) {
			LocalDate start = date;
			LocalDate endOfThisMonth = date.plusMonths(1).withDayOfMonth(1).minusDays(1);
			LocalDate end = endDate.isBefore(endOfThisMonth) ? endDate : endOfThisMonth;
			queryExecutor.accept(start, end);
			date = end.plusDays(1);
		}
	}
	
	public void splitByYear(LocalDate startDate, LocalDate endDate, BiConsumer<LocalDate, LocalDate> queryExecutor) {
		LocalDate date = startDate;
		while(!date.isAfter(endDate)) {
			LocalDate start = date;
			LocalDate endOfThisYear = LocalDate.of(date.getYear(), 12, 31);
			LocalDate end = endDate.isBefore(endOfThisYear) ? endDate : endOfThisYear;
			queryExecutor.accept(start, end);
			date = end.plusDays(1);
		}
	}	
	
}
