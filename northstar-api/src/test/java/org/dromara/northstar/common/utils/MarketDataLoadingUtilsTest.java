package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.dromara.northstar.common.utils.MarketDataLoadingUtils;
import org.junit.jupiter.api.Test;

class MarketDataLoadingUtilsTest {
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();

	@Test
	void testGetCurrentTradeDayForFirstLoad() {
		// 周一至四夜盘情况
		LocalDateTime sometime = LocalDateTime.of(LocalDate.of(2022, 5, 31), LocalTime.of(22, 0));
		assertThat(utils.getCurrentTradeDay(sometime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli(), true)).isEqualTo(LocalDate.of(2022, 6, 1));
		
		// 日盘情况
		LocalDateTime sometime2 = LocalDateTime.of(LocalDate.of(2022, 5, 31), LocalTime.of(15, 0));
		assertThat(utils.getCurrentTradeDay(sometime2.toInstant(ZoneOffset.ofHours(8)).toEpochMilli(), true)).isEqualTo(LocalDate.of(2022, 5, 31));
		
		// 周五晚夜盘情况
		LocalDateTime sometime3 = LocalDateTime.of(LocalDate.of(2022, 5, 27), LocalTime.of(22, 0));
		assertThat(utils.getCurrentTradeDay(sometime3.toInstant(ZoneOffset.ofHours(8)).toEpochMilli(), true)).isEqualTo(LocalDate.of(2022, 5, 30));
	}
	
	@Test
	void testGetCurrentTradeDay() {
		// 周一至四夜盘情况
		LocalDateTime sometime = LocalDateTime.of(LocalDate.of(2022, 5, 31), LocalTime.of(22, 0));
		assertThat(utils.getCurrentTradeDay(sometime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli(), false)).isEqualTo(LocalDate.of(2022, 5, 31));
		
		// 日盘情况
		LocalDateTime sometime2 = LocalDateTime.of(LocalDate.of(2022, 5, 31), LocalTime.of(15, 0));
		assertThat(utils.getCurrentTradeDay(sometime2.toInstant(ZoneOffset.ofHours(8)).toEpochMilli(), false)).isEqualTo(LocalDate.of(2022, 5, 31));
		
		// 周五晚夜盘情况
		LocalDateTime sometime3 = LocalDateTime.of(LocalDate.of(2022, 5, 27), LocalTime.of(22, 0));
		assertThat(utils.getCurrentTradeDay(sometime3.toInstant(ZoneOffset.ofHours(8)).toEpochMilli(), false)).isEqualTo(LocalDate.of(2022, 5, 27));
	}
	
	@Test
	void testGetLastDayOfLastWeek() {
		// 周一日盘情况
		LocalDateTime sometime = LocalDateTime.of(LocalDate.of(2022, 5, 30), LocalTime.of(9, 0));
		assertThat(utils.getFridayOfLastWeek(sometime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli())).isEqualTo(LocalDate.of(2022, 5, 27));
				
		// 周五晚夜盘情况
		LocalDateTime sometime2 = LocalDateTime.of(LocalDate.of(2022, 5, 27), LocalTime.of(22, 0));
		assertThat(utils.getFridayOfLastWeek(sometime2.toInstant(ZoneOffset.ofHours(8)).toEpochMilli())).isEqualTo(LocalDate.of(2022, 5, 20));
	}

}
