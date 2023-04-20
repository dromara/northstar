package org.dromara.northstar.gateway.common.domain.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.gateway.common.domain.time.GenericTradeTime;
import org.dromara.northstar.gateway.common.domain.time.PeriodHelper;
import org.dromara.northstar.gateway.common.domain.time.PeriodSegment;
import org.dromara.northstar.gateway.common.domain.time.TradeTimeDefinition;
import org.junit.jupiter.api.Test;

class PeriodHelperTest {
	
	TradeTimeDefinition demo = new TradeTimeDefinition() {
		@Override
		public List<PeriodSegment> tradeTimeSegments() {
			return List.of(
					new PeriodSegment(LocalTime.of(21, 0), LocalTime.of(2, 30)),
					new PeriodSegment(LocalTime.of(9, 1), LocalTime.of(10, 15)),
					new PeriodSegment(LocalTime.of(10, 31), LocalTime.of(11, 30)),
					new PeriodSegment(LocalTime.of(13, 31), LocalTime.of(15, 00))
				);
		}

	};
	
	TradeTimeDefinition general = new GenericTradeTime();
	
	PeriodHelper h1 = new PeriodHelper(1, demo, false);
	PeriodHelper h2 = new PeriodHelper(1, demo, true);
	PeriodHelper h3 = new PeriodHelper(60, demo, true);
	
	PeriodHelper h4 = new PeriodHelper(1, general, false);

	@Test
	void testTimeFrame() {
		List<LocalTime> timeFrame1 = h1.getRunningBaseTimeFrame();
		List<LocalTime> timeFrame2 = h2.getRunningBaseTimeFrame();
		List<LocalTime> timeFrame3 = h3.getRunningBaseTimeFrame();
		
		assertThat(timeFrame1).hasSize(556);
		assertThat(timeFrame2).hasSize(555);
		assertThat(timeFrame3).hasSize(10);
	}
	
	@Test
	void testTimeFrame2() {
		List<LocalTime> timeFrame4 = h4.getRunningBaseTimeFrame();
		assertThat(timeFrame4).hasSize(1440);
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
