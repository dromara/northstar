package tech.quantit.northstar.gateway.playback.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.IHolidayManager;

class CtpPlaybackClockTest {

	CtpPlaybackClock clock;
	
	IHolidayManager holidayMgr;
	
	@BeforeEach
	void prepare() {
		holidayMgr = mock(IHolidayManager.class);
	}
	
	@Test
	void testWithinSection() {
		when(holidayMgr.isHoliday(any(LocalDateTime.class))).thenReturn(false);
		clock = new CtpPlaybackClock(holidayMgr, LocalDateTime.of(2022, 6, 28, 21, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 28, 21, 1));
	}
	
	@Test
	void testCrossSection() {
		when(holidayMgr.isHoliday(any(LocalDateTime.class))).thenReturn(true);
		when(holidayMgr.isHoliday(LocalDateTime.of(2022, 6, 27, 9, 0, 0))).thenReturn(false);
		clock = new CtpPlaybackClock(holidayMgr, LocalDateTime.of(2022, 6, 25, 2, 29, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 27, 9, 0, 0));
	}

	@Test
	void testCrossSection2() {
		when(holidayMgr.isHoliday(any(LocalDateTime.class))).thenReturn(false);
		clock = new CtpPlaybackClock(holidayMgr, LocalDateTime.of(2022, 6, 27, 11, 29, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 27, 13, 30, 0));
	}
	
	@Test
	void testCrossSection3() {
		when(holidayMgr.isHoliday(any(LocalDateTime.class))).thenReturn(false);
		clock = new CtpPlaybackClock(holidayMgr, LocalDateTime.of(2022, 6, 27, 15, 14, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 27, 21, 0, 0));
	}
	
	@Test
	void testCrossSection4() {
		when(holidayMgr.isHoliday(any(LocalDateTime.class))).thenReturn(false);
		clock = new CtpPlaybackClock(holidayMgr, LocalDateTime.of(2022, 6, 27, 23, 59, 0));
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 28, 0, 0, 0));
	}
}
