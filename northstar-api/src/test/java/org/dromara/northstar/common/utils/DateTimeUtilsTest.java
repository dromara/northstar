package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class DateTimeUtilsTest {

	@Test
	void testTimeCache() {
		LocalTime t1 = LocalTime.of(1, 13);
		LocalTime t2 = LocalTime.of(1, 13);
		assertThat(t1).isNotSameAs(t2);

		LocalTime t3 = DateTimeUtils.fromCacheTime(t1);
		LocalTime t4 = DateTimeUtils.fromCacheTime(t2);
		assertThat(t3).isSameAs(t4);
	}

	@Test
	void testWeekOfYear() {
		int weekOfYear = DateTimeUtils.weekOfYear(LocalDate.of(2026, 1, 1));
		assertThat(weekOfYear).isEqualTo(202601);
		
		LocalDate date = DateTimeUtils.fromWeekOfYear(weekOfYear, DayOfWeek.FRIDAY);
		assertThat(date).isEqualTo(LocalDate.of(2026, 1, 2));
		
		LocalDate date2 = DateTimeUtils.fromWeekOfYear(weekOfYear, DayOfWeek.SUNDAY);
		assertThat(date2).isEqualTo(LocalDate.of(2026, 1, 4));
	}
	
	@Test
	void testWeekOfYear2() {
		for(int i=0; i<10; i++) {
			LocalDate date = LocalDate.of(2025, 12, 1).plusWeeks(i);
			int weekOfYear = DateTimeUtils.weekOfYear(date);
			System.out.println(String.format("%d %s", weekOfYear, date));
		}
	}
	
	@Test
	void testWeekOfYear3() {
		LocalDate date1 = DateTimeUtils.fromWeekOfYear(202601, DayOfWeek.MONDAY);
		assertThat(date1).isEqualTo(LocalDate.of(2025, 12, 29));
	}
}
