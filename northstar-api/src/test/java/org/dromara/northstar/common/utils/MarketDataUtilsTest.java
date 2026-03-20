package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.junit.jupiter.api.Test;

class MarketDataUtilsTest {
	
	TradeTimeDefinition ttd = TradeTimeDefinition.builder()
			.timeSlots(List.of(
						TimeSlot.builder().start(LocalTime.of(21, 0)).end(LocalTime.of(23, 0)).build(),
						TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(15, 0)).build()
					))
			.build();
	
	Contract c = Contract.builder()
			.contractDefinition(ContractDefinition.builder()
					.tradeTimeDef(ttd)
					.build())
			.build();

	@Test
	void testIsOpenningBar() {
		LocalTime t1 = LocalTime.of(21, 0);
		LocalTime t2 = LocalTime.of(9, 0);
		
		Bar b1 = Bar.builder()
				.actionDay(LocalDate.now())
				.actionTime(t1)
				.contract(c)
				.build();
		Bar b2 = Bar.builder()
				.actionDay(LocalDate.now())
				.actionTime(t2)
				.contract(c)
				.build();
		
		assertThat(MarketDataUtils.isOpenningBar(b1)).isTrue();
		assertThat(MarketDataUtils.isOpenningBar(b2)).isFalse();
	}
	
	@Test
	void testIsStartingBar() {
		LocalTime t1 = LocalTime.of(21, 0);
		LocalTime t2 = LocalTime.of(9, 0);
		LocalTime t3 = LocalTime.of(9, 1);
		
		Bar b1 = Bar.builder()
				.actionDay(LocalDate.now())
				.actionTime(t1)
				.contract(c)
				.build();
		Bar b2 = Bar.builder()
				.actionDay(LocalDate.now())
				.actionTime(t2)
				.contract(c)
				.build();
		Bar b3 = Bar.builder()
				.actionDay(LocalDate.now())
				.actionTime(t3)
				.contract(c)
				.build();
		
		assertThat(MarketDataUtils.isStartingBar(b1)).isTrue();
		assertThat(MarketDataUtils.isStartingBar(b2)).isTrue();
		assertThat(MarketDataUtils.isStartingBar(b3)).isFalse();
	}

	@Test
	void testIsEndingBar() {
		LocalTime t1 = LocalTime.of(23, 0);
		LocalTime t2 = LocalTime.of(15, 0);
		LocalTime t3 = LocalTime.of(14, 59);
		
		Bar b1 = Bar.builder()
				.actionDay(LocalDate.now())
				.actionTime(t1)
				.contract(c)
				.build();
		Bar b2 = Bar.builder()
				.actionDay(LocalDate.now())
				.actionTime(t2)
				.contract(c)
				.build();
		Bar b3 = Bar.builder()
				.actionDay(LocalDate.now())
				.actionTime(t3)
				.contract(c)
				.build();
		
		assertThat(MarketDataUtils.isEndingBar(b1)).isTrue();
		assertThat(MarketDataUtils.isEndingBar(b2)).isTrue();
		assertThat(MarketDataUtils.isEndingBar(b3)).isFalse();
	}

	@Test
	void testIsOpenningTick() {
		Tick t1 = Tick.builder()
				.actionDay(LocalDate.now())
				.actionTime(LocalTime.of(21, 0, 5))
				.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(LocalDate.now(), LocalTime.of(21, 0, 5))))
				.contract(c)
				.build();
		
		assertThat(MarketDataUtils.isOpenningTick(t1, 6)).isTrue();
		assertThat(MarketDataUtils.isOpenningTick(t1, 5)).isFalse();
	}

	@Test
	void testSecondsToWholeMin() {
		Tick t1 = Tick.builder()
				.actionDay(LocalDate.now())
				.actionTime(LocalTime.of(21, 0, 5))
				.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(LocalDate.now(), LocalTime.of(21, 0, 5))))
				.contract(c)
				.build();
		
		assertThat(MarketDataUtils.secondsToWholeMin(t1)).isEqualTo(55);
	}

	@Test
	void testMinutesToDayEnd() {
		LocalTime t1 = LocalTime.of(14, 59);
		
		Bar b1 = Bar.builder()
				.tradingDay(LocalDate.now())
				.actionDay(LocalDate.now())
				.actionTime(t1)
				.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(LocalDate.now(), t1)))
				.contract(c)
				.build();
		
		assertThat(MarketDataUtils.minutesToDayEnd(b1)).isEqualTo(1);
	}

}
