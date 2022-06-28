package tech.quantit.northstar.gateway.playback.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
		long time = LocalDateTime.of(2022, 6, 28, 21, 0, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		clock = new CtpPlaybackClock(holidayMgr, time);
		assertThat(clock.nextMarketMinute()).isEqualTo(time + 60000);
	}
	
	@Test
	void testCrossSection() {
		when(holidayMgr.isHoliday(any(LocalDateTime.class))).thenReturn(true);
		when(holidayMgr.isHoliday(LocalDateTime.of(2022, 6, 27, 9, 0, 0))).thenReturn(false);
		long time = LocalDateTime.of(2022, 6, 25, 2, 29, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		clock = new CtpPlaybackClock(holidayMgr, time);
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 27, 9, 0, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
	}

	@Test
	void testCrossSection2() {
		when(holidayMgr.isHoliday(any(LocalDateTime.class))).thenReturn(false);
		long time = LocalDateTime.of(2022, 6, 27, 11, 29, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		clock = new CtpPlaybackClock(holidayMgr, time);
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 27, 13, 30, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
	}
	
	@Test
	void testCrossSection3() {
		when(holidayMgr.isHoliday(any(LocalDateTime.class))).thenReturn(false);
		long time = LocalDateTime.of(2022, 6, 27, 15, 14, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		clock = new CtpPlaybackClock(holidayMgr, time);
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 27, 21, 0, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
	}
	
	@Test
	void testCrossSection4() {
		when(holidayMgr.isHoliday(any(LocalDateTime.class))).thenReturn(false);
		long time = LocalDateTime.of(2022, 6, 27, 23, 59, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		clock = new CtpPlaybackClock(holidayMgr, time);
		assertThat(clock.nextMarketMinute()).isEqualTo(LocalDateTime.of(2022, 6, 28, 0, 0, 0).toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
	}
}
