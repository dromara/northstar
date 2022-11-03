package tech.quantit.northstar.gateway.api.domain.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class PeriodHelperTest {

	PeriodHelper h1 = new PeriodHelper(60, new CnFtComTradeTime3(), LocalTime.of(21, 0));
	PeriodHelper h2 = new PeriodHelper(60, new CnFtComTradeTime3());
	
	
	@Test
	void test() {
		assertThat(h1.withinTheSamePeriod(LocalTime.of(21, 0), LocalTime.of(22, 0))).isTrue();
		assertThat(h2.withinTheSamePeriod(LocalTime.of(21, 0), LocalTime.of(22, 0))).isFalse();
		
		assertThat(h1.withinTheSamePeriod(LocalTime.of(2, 1), LocalTime.of(9, 1))).isTrue();
	}

	@Test
	void testTimeFrame() {
		List<LocalTime> timeFrame1 = h1.getRunningBaseTimeFrame();
		List<LocalTime> timeFrame2 = h2.getRunningBaseTimeFrame();
		
		assertThat(timeFrame1).hasSize(556);
		assertThat(timeFrame2).hasSize(555);
	}
}
