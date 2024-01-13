package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.junit.jupiter.api.Test;

class TradeTimeUtilTest {

	@Test
	void test() {
		TradeTimeDefinition ttd = TradeTimeDefinition.builder()
				.timeSlots(List.of(
						TimeSlot.builder().start(LocalTime.of(21, 0)).end(LocalTime.of(2, 30)).build(),
						TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(11, 30)).build(),
						TimeSlot.builder().start(LocalTime.of(13, 0)).end(LocalTime.of(15, 0)).build()
						))
				.build();
		TradeTimeUtil util = new TradeTimeUtil(ttd);
		
		assertThat(util.withinTradeTime(LocalTime.of(20, 59))).isFalse();
		assertThat(util.withinTradeTime(LocalTime.of(21, 0))).isTrue();
		assertThat(util.withinTradeTime(LocalTime.of(23, 59))).isTrue();
		assertThat(util.withinTradeTime(LocalTime.of(0, 0))).isTrue();
		assertThat(util.withinTradeTime(LocalTime.of(0, 1))).isTrue();
		assertThat(util.withinTradeTime(LocalTime.of(2, 30))).isTrue();
		assertThat(util.withinTradeTime(LocalTime.of(2, 30, 1))).isFalse();
		assertThat(util.withinTradeTime(LocalTime.of(9, 0))).isTrue();
		assertThat(util.withinTradeTime(LocalTime.of(11, 30))).isTrue();
		assertThat(util.withinTradeTime(LocalTime.of(13, 30))).isTrue();
		assertThat(util.withinTradeTime(LocalTime.of(11, 30, 1))).isFalse();
		assertThat(util.withinTradeTime(LocalTime.of(12, 59, 59))).isFalse();
	}

}
