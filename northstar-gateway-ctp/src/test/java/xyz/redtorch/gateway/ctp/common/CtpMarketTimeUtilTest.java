package xyz.redtorch.gateway.ctp.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.TickType;
import tech.quantit.northstar.common.utils.MarketDateTimeUtil;

class CtpMarketTimeUtilTest {
	
	private MarketDateTimeUtil util = new CtpDateTimeUtil();

	@Test
	public void test() {
		assertThat(util.resolveTickType("rb2202", LocalTime.of(2, 29, 1))).isEqualTo(TickType.NORMAL_TICK);
		assertThat(util.resolveTickType("rb2202", LocalTime.of(15, 0))).isEqualTo(TickType.CLOSING_TICK);
		assertThat(util.resolveTickType("rb2202", LocalTime.of(14, 59, 59, 100000000))).isEqualTo(TickType.CLOSING_TICK);
		assertThat(util.resolveTickType("rb2202", LocalTime.of(15, 0, 0, 500000000))).isEqualTo(TickType.NON_OPENING_TICK);
		assertThat(util.resolveTickType("rb2202", LocalTime.of(8, 58))).isEqualTo(TickType.NON_OPENING_TICK);
		assertThat(util.resolveTickType("rb2202", LocalTime.of(8, 59))).isEqualTo(TickType.PRE_OPENING_TICK);
		assertThat(util.resolveTickType("rb2202", LocalTime.of(10, 14, 58))).isEqualTo(TickType.NORMAL_TICK);
		assertThat(util.resolveTickType("rb2202", LocalTime.of(13, 30, 1))).isEqualTo(TickType.NORMAL_TICK);
		
		assertThat(util.resolveTickType("T2206", LocalTime.of(15, 10, 1))).isEqualTo(TickType.NORMAL_TICK);
		assertThat(util.resolveTickType("TS2206", LocalTime.of(15, 10, 1))).isEqualTo(TickType.NORMAL_TICK);
		assertThat(util.resolveTickType("TF2206", LocalTime.of(15, 10, 1))).isEqualTo(TickType.NORMAL_TICK);
		
		assertThat(util.resolveTickType("T2206", LocalTime.of(9, 25, 1))).isEqualTo(TickType.PRE_OPENING_TICK);
		assertThat(util.resolveTickType("TS2206", LocalTime.of(8, 10, 1))).isEqualTo(TickType.NON_OPENING_TICK);
		assertThat(util.resolveTickType("TF2206", LocalTime.of(15, 30, 1))).isEqualTo(TickType.NON_OPENING_TICK);
		assertThat(util.resolveTickType("TF2206", LocalTime.of(15, 15, 0))).isEqualTo(TickType.CLOSING_TICK);
	}

}
