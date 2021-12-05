package tech.quantit.northstar.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HolidayManagerTest {
	
	private HolidayManager mgr = new HolidayManager();
	
	@BeforeEach
	void prepare() throws Exception {
		mgr.holidayStrs = new String[] {"20210614", "20210920", "20210921"};
		mgr.afterPropertiesSet();
	}

	@Test
	void testIsHoliday() {
		assertThat(mgr.isHoliday(LocalDateTime.of(2021, 6, 11, 21, 0))).isTrue();
		
		assertThat(mgr.isHoliday(LocalDateTime.of(2021, 6, 15, 0, 0))).isTrue();
		
		assertThat(mgr.isHoliday(LocalDateTime.of(2021, 6, 15, 8, 30))).isFalse();
		
		assertThat(mgr.isHoliday(LocalDateTime.of(2021, 6, 28, 01, 0))).isTrue();
		
		assertThat(mgr.isHoliday(LocalDateTime.of(2021, 9, 17, 21, 0))).isTrue();
		
		assertThat(mgr.isHoliday(LocalDateTime.of(2021, 9, 21, 21, 0))).isTrue();
		
		assertThat(mgr.isHoliday(LocalDateTime.of(2021, 9, 22, 0, 0))).isTrue();
		
		assertThat(mgr.isHoliday(LocalDateTime.of(2021, 9, 22, 8, 30))).isFalse();
	}

}
