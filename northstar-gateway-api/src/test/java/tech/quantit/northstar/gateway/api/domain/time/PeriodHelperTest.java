package tech.quantit.northstar.gateway.api.domain.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class PeriodHelperTest {

	PeriodHelper h1 = new PeriodHelper(60, new CnFtComTradeTime3());
	PeriodHelper h2 = new PeriodHelper(60, new CnFtComTradeTime3(), true);
	
	

	@Test
	void testTimeFrame() {
		List<LocalTime> timeFrame1 = h1.getRunningBaseTimeFrame();
		List<LocalTime> timeFrame2 = h2.getRunningBaseTimeFrame();
		
		assertThat(timeFrame1).hasSize(556);
		assertThat(timeFrame2).hasSize(555);
	}
	
	@Test
	void testEndOfSection() {
		assertThat(h1.isEndOfSection(LocalTime.of(2, 30))).isTrue();
		assertThat(h1.isEndOfSection(LocalTime.of(10, 15))).isTrue();
		assertThat(h1.isEndOfSection(LocalTime.of(11, 30))).isTrue();
		assertThat(h1.isEndOfSection(LocalTime.of(15, 0))).isTrue();
		
		assertThat(h1.isEndOfSection(LocalTime.of(23, 0))).isFalse();
		assertThat(h1.isEndOfSection(LocalTime.of(14, 59))).isFalse();
	}
}
