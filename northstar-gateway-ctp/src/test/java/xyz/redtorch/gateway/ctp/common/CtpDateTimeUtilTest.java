package xyz.redtorch.gateway.ctp.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class CtpDateTimeUtilTest {
	
	CtpDateTimeUtil util = new CtpDateTimeUtil();

	@Test
	void test() {
		assertThat(util.getTradingDay(LocalDateTime.of(2022, 3, 18, 21, 0, 0))).isEqualTo(LocalDate.of(2022, 3, 21));
		assertThat(util.getTradingDay(LocalDateTime.of(2022, 3, 21, 21, 0, 0))).isEqualTo(LocalDate.of(2022, 3, 22));
		assertThat(util.getTradingDay(LocalDateTime.of(2022, 3, 18, 9, 0, 0))).isEqualTo(LocalDate.of(2022, 3, 18));
		assertThat(util.getTradingDay(LocalDateTime.of(2022, 3, 21, 9, 0, 0))).isEqualTo(LocalDate.of(2022, 3, 21));
	}

	@Test
	void testOpenTime() {
		assertThat(util.isOpeningTime("rb2210", LocalTime.of(8, 55))).isFalse();
		assertThat(util.isOpeningTime("rb2210", LocalTime.of(15, 5))).isFalse();
		assertThat(util.isOpeningTime("rb2210", LocalTime.of(20, 55))).isFalse();
		assertThat(util.isOpeningTime("T2212", LocalTime.of(15, 16))).isFalse();
		assertThat(util.isOpeningTime("rb2210", LocalTime.of(9, 0))).isTrue();
		assertThat(util.isOpeningTime("rb2210", LocalTime.of(21, 0))).isTrue();
		assertThat(util.isOpeningTime("rb2210", LocalTime.of(2, 30))).isTrue();
		assertThat(util.isOpeningTime("rb2210", LocalTime.of(15, 0))).isTrue();
		assertThat(util.isOpeningTime("T2212", LocalTime.of(15, 15))).isTrue();
	}
}
