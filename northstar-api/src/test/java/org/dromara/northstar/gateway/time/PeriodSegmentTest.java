package org.dromara.northstar.gateway.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.dromara.northstar.gateway.model.PeriodSegment;
import org.junit.jupiter.api.Test;

class PeriodSegmentTest {

	@Test
	void test() {
		PeriodSegment ps = new PeriodSegment(LocalTime.of(21, 0), LocalTime.of(2, 30));
		assertThat(ps.withinPeriod(LocalTime.of(23, 0))).isTrue();
		assertThat(ps.withinPeriod(LocalTime.of(1, 0))).isTrue();
		assertThat(ps.withinPeriod(LocalTime.of(3, 0))).isFalse();
		assertThat(ps.withinPeriod(LocalTime.of(20, 0))).isFalse();
		
		PeriodSegment ps2 = new PeriodSegment(LocalTime.of(13, 0), LocalTime.of(15, 30));
		assertThat(ps2.withinPeriod(LocalTime.of(13, 0))).isTrue();
		assertThat(ps2.withinPeriod(LocalTime.of(15, 30))).isTrue();
		assertThat(ps2.withinPeriod(LocalTime.of(15, 31))).isFalse();
		assertThat(ps2.withinPeriod(LocalTime.of(12, 59))).isFalse();
	}

}
