package org.dromara.northstar.common.model.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class TradeTimeDefinitionTest {

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
	}

}
