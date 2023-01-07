package tech.quantit.northstar.gateway.api.domain.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;

class DateUtilsTest {

	@Test
	void test() {
		assertThat(DateUtils.numOfWeekDay(2023, Month.MARCH, 2, DayOfWeek.SUNDAY)).isEqualTo(LocalDate.of(2023, 3, 12));
		assertThat(DateUtils.numOfWeekDay(2022, Month.MAY, 2, DayOfWeek.SUNDAY)).isEqualTo(LocalDate.of(2022, 5, 8));
		assertThat(DateUtils.numOfWeekDay(2022, Month.NOVEMBER, 4, DayOfWeek.THURSDAY)).isEqualTo(LocalDate.of(2022, 11, 24));
		assertThat(DateUtils.numOfWeekDay(2022, Month.OCTOBER, 5, DayOfWeek.MONDAY)).isEqualTo(LocalDate.of(2022, 10, 31));
	}

}
