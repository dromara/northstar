package org.dromara.northstar.gateway.playback.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CtpPlaybackClockTest {

	CtpPlaybackClock clock;
	
	@Test
	void testWithinSection() {
		clock = new CtpPlaybackClock(LocalDateTime.of(2022, 6, 28, 21, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 28, 21, 1));
	}
	
	@Test
	void testCrossSection() {
		clock = new CtpPlaybackClock(LocalDateTime.of(2022, 6, 25, 2, 29, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 27, 9, 0, 0));
	}

	@Test
	void testCrossSection2() {
		clock = new CtpPlaybackClock(LocalDateTime.of(2022, 6, 27, 11, 29, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 27, 13, 30, 0));
	}
	
	@Test
	void testCrossSection3() {
		clock = new CtpPlaybackClock(LocalDateTime.of(2022, 6, 27, 15, 14, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 27, 21, 0, 0));
	}
	
	@Test
	void testCrossSection4() {
		clock = new CtpPlaybackClock(LocalDateTime.of(2022, 6, 27, 23, 59, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 28, 0, 0, 0));
	}
}
