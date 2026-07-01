package org.dromara.northstar.common.model.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class TradeTimeDefinitionTest {
	
	TradeTimeDefinition ttd = TradeTimeDefinition.builder().timeSlots(List.of(
			TimeSlot.builder().start(LocalTime.of(21, 0)).end(LocalTime.of(2, 30)).build(),
			TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(11, 30)).build(),
			TimeSlot.builder().start(LocalTime.of(13, 0)).end(LocalTime.of(15, 0)).build()
			)).build();

	@Test
	void test() {
		TradeTimeDefinition tt1 = TradeTimeDefinition.builder()
				.timeSlots(List.of(
						TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(18, 0)).build(),
						TimeSlot.builder().start(LocalTime.of(20, 0)).end(LocalTime.of(23, 30)).build()
						))
				.build();
		TradeTimeDefinition tt2 = TradeTimeDefinition.builder()
				.timeSlots(List.of(
						TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(18, 0)).build(),
						TimeSlot.builder().start(LocalTime.of(20, 0)).end(LocalTime.of(23, 30)).build()
						))
				.build();
		assertThat(tt1).isEqualTo(tt2);
		assertThat(tt1.weekendClosure()).isNotNull();
	}

	@Test
	void test2() {
		
		assertThat(ttd.withinTradeTime(LocalTime.of(20, 59))).isFalse();
		assertThat(ttd.withinTradeTime(LocalTime.of(21, 0))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(23, 59))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(0, 0))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(0, 1))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(2, 30))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(2, 30, 1))).isFalse();
		assertThat(ttd.withinTradeTime(LocalTime.of(9, 0))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(11, 30))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(13, 30))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(11, 30, 1))).isFalse();
		assertThat(ttd.withinTradeTime(LocalTime.of(12, 59, 59))).isFalse();
		assertThat(ttd.withinTradeTime(LocalTime.of(22, 59))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(11, 29))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(14, 59))).isTrue();
		// 边界秒级校验
		assertThat(ttd.withinTradeTime(LocalTime.of(8, 59, 59))).isFalse();
		assertThat(ttd.withinTradeTime(LocalTime.of(9, 0, 1))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(11, 29, 59))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(15, 0, 1))).isFalse();
		assertThat(ttd.withinTradeTime(LocalTime.of(20, 59, 59))).isFalse();
		assertThat(ttd.withinTradeTime(LocalTime.of(21, 0, 1))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(2, 29, 59))).isTrue();
		assertThat(ttd.withinTradeTime(LocalTime.of(2, 30, 1))).isFalse();
	}

	@Test
	void testNearestTradeTime() {
		assertThat(ttd.nearestTradeTime(LocalTime.of(20, 59))).isEqualTo(LocalTime.of(21, 0));
		assertThat(ttd.nearestTradeTime(LocalTime.of(8, 59))).isEqualTo(LocalTime.of(9, 0));
		assertThat(ttd.nearestTradeTime(LocalTime.of(9, 0))).isEqualTo(LocalTime.of(9, 0));
		assertThat(ttd.nearestTradeTime(LocalTime.of(2, 31))).isEqualTo(LocalTime.of(2, 30));
	}
}
