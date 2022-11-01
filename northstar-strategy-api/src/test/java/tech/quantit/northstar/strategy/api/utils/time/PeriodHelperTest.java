package tech.quantit.northstar.strategy.api.utils.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.strategy.api.utils.time.trade.CnFtComTradeTime3;

class PeriodHelperTest {

	PeriodHelper h1 = new PeriodHelper(60, new CnFtComTradeTime3(), LocalTime.of(21, 0));
	PeriodHelper h2 = new PeriodHelper(60, new CnFtComTradeTime3());
	
	
	@Test
	void test() {
		assertThat(h1.withinTheSamePeriod(LocalTime.of(21, 0), LocalTime.of(22, 0))).isTrue();
		assertThat(h2.withinTheSamePeriod(LocalTime.of(21, 0), LocalTime.of(22, 0))).isFalse();
		
		assertThat(h1.withinTheSamePeriod(LocalTime.of(2, 1), LocalTime.of(9, 1))).isTrue();
	}

}
