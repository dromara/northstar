package xyz.redtorch.gateway.ctp.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

}
