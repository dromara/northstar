package xyz.redtorch.gateway.ctp.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.TickType;

class CtpMarketTimeUtilTest {
	
	CtpMarketTimeUtil util = new CtpMarketTimeUtil();

	@Test
	public void test() {
		assertThat(util.resolveTickType(LocalTime.of(2, 29, 1))).isEqualTo(TickType.NORMAL_TICK);
		assertThat(util.resolveTickType(LocalTime.of(15, 0))).isEqualTo(TickType.CLOSING_TICK);
		assertThat(util.resolveTickType(LocalTime.of(8, 58))).isEqualTo(TickType.NON_OPENING_TICK);
		assertThat(util.resolveTickType(LocalTime.of(8, 59))).isEqualTo(TickType.PRE_OPENING_TICK);
		assertThat(util.resolveTickType(LocalTime.of(10, 14, 58))).isEqualTo(TickType.NORMAL_TICK);
		assertThat(util.resolveTickType(LocalTime.of(13, 30, 1))).isEqualTo(TickType.NORMAL_TICK);
	}

}
