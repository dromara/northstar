package tech.quantit.northstar.gateway.api.domain.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class BarMergingClockTest {

	BarMergingClock clock = new BarMergingClock(List.of(
			LocalTime.of(22, 0), LocalTime.of(23, 0), LocalTime.of(10, 0), 
			LocalTime.of(11, 15), LocalTime.of(14, 15), LocalTime.of(15, 0)));
	
	@Test
	void testAdjustTime() {
		assertThat(clock.adjustTime(LocalTime.of(21, 0))).isFalse();
		assertThat(clock.adjustTime(LocalTime.of(22, 0))).isFalse();
		assertThat(clock.adjustTime(LocalTime.of(10, 30))).isTrue();
	}
	
	@Test
	void test() {
		assertThat(clock.adjustTime(LocalTime.of(21, 0))).isFalse();
		assertThat(clock.currentTimeBucket()).isEqualTo(LocalTime.of(22, 0));
		assertThat(clock.next()).isEqualTo(LocalTime.of(23, 0));
		assertThat(clock.currentTimeBucket()).isEqualTo(LocalTime.of(23, 0));
		
		assertThat(clock.adjustTime(LocalTime.of(10, 30))).isTrue();
		assertThat(clock.currentTimeBucket()).isEqualTo(LocalTime.of(11, 15));
		assertThat(clock.next()).isEqualTo(LocalTime.of(14, 15));
	}

}
