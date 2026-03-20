package org.dromara.northstar.gateway.playback.ticker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.CommonUtils;
import org.junit.jupiter.api.Test;

class SimplePriceSimulationTest {
	
	SimplePriceSimulation sps = new SimplePriceSimulation();

	ContractDefinition cd = ContractDefinition.builder()
			.tradeTimeDef(TradeTimeDefinition.builder()
					.timeSlots(List.of(TimeSlot.builder()
							.start(LocalTime.of(9, 0))
							.end(LocalTime.of(15, 00))
							.build()))
					.build())
			.build();
	
	Contract c = Contract.builder()
			.priceTick(1)
			.contractDefinition(cd)
			.build();
	
	Contract c2 = Contract.builder()
			.priceTick(1)
			.contractDefinition(cd)
			.usingStartTimeInBarTime(true)
			.build();
	
	@Test
	void testTicks() {
		Bar bar = Bar.builder()
				.contract(c)
				.actionDay(LocalDate.of(2024, 12, 1))
				.actionTime(LocalTime.of(9, 0))
				.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(LocalDate.of(2024, 12, 1), LocalTime.of(9, 0))))
				.openPrice(5000)
				.highPrice(5005)
				.lowPrice(4998)
				.closePrice(5000)
				.openInterestDelta(31)
				.volumeDelta(2000)
				.build();
		assertThat(sps.generateFrom(bar)).isEmpty();
		
		long t = CommonUtils.localDateTimeToMills(LocalDateTime.of(LocalDate.of(2024, 12, 1), LocalTime.of(9, 1)));
		Bar bar2 = Bar.builder()
				.contract(c)
				.actionDay(LocalDate.of(2024, 12, 1))
				.actionTime(LocalTime.of(9, 1))
				.actionTimestamp(t)
				.openPrice(5000)
				.highPrice(5005)
				.lowPrice(4998)
				.closePrice(5000)
				.openInterestDelta(31)
				.volumeDelta(2000)
				.build();
		assertThat(sps.generateFrom(bar2).getFirst().timestamp()).isEqualTo(t - 60000);
	}

	@Test
	void testTickMatching() {
		Bar bar = Bar.builder()
				.contract(c)
				.actionDay(LocalDate.of(2024, 12, 1))
				.actionTime(LocalTime.of(9, 1))
				.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(LocalDate.of(2024, 12, 1), LocalTime.of(9, 1))))
				.openPrice(5000)
				.highPrice(5005)
				.lowPrice(4998)
				.closePrice(5000)
				.openInterestDelta(31)
				.volumeDelta(2000)
				.build();
		
		Bar bar2 = Bar.builder()
				.contract(c2)
				.actionDay(LocalDate.of(2024, 12, 1))
				.actionTime(LocalTime.of(9, 0))
				.actionTimestamp(CommonUtils.localDateTimeToMills(LocalDateTime.of(LocalDate.of(2024, 12, 1), LocalTime.of(9, 0))))
				.openPrice(5000)
				.highPrice(5005)
				.lowPrice(4998)
				.closePrice(5000)
				.openInterestDelta(31)
				.volumeDelta(2000)
				.build();
		
		List<TickEntry> ticks = sps.generateFrom(bar);
		List<TickEntry> ticks2 = sps.generateFrom(bar2);
		
		assertThat(ticks.size()).isEqualTo(ticks2.size());
		for(int i=0; i<ticks.size(); i++) {
			assertThat(ticks.get(i).timestamp()).isEqualTo(ticks2.get(i).timestamp());
		}
	}
}
