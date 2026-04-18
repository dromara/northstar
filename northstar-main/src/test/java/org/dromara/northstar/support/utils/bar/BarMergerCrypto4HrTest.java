package org.dromara.northstar.support.utils.bar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.junit.jupiter.api.Test;

import org.dromara.northstar.common.constant.Interval;

class BarMergerCrypto4HrTest {
	
	List<TimeSlot> times = List.of(
			TimeSlot.builder().start(LocalTime.of(8, 0)).end(LocalTime.of(7, 59, 59)).build()
		);
	
    Contract contract = Contract.builder()
    		.unifiedSymbol("BTCUSDT@BINANCE@SPOT")
    		.usingStartTimeInBarTime(true)
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(times)
    						.build())
    				.build())
    		.build();
    
	@Test
	void test4HrNormal1DayC1() {
		BarUtils utils = new BarUtils(5000, contract, Interval.M1);
		BarMerger merger = new BarMerger(Interval.H4, contract);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 10), LocalTime.of(8, 0));
		LocalDateTime endDateTime = LocalDateTime.of(LocalDate.of(2024, 12, 12), LocalTime.of(8, 0));
		List<Bar> results = new LinkedList<>();
    		merger.addListener(bar -> results.add(bar));
		while(!ldt.isAfter(endDateTime)) {
			Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
			merger.onBar(bar);
			ldt = ldt.plusMinutes(1);
		}
		
		assertThat(results).hasSize(12);
		assertThat(results.get(0).actionDay()).isEqualTo("2024-12-10");
		assertThat(results.get(0).actionTime()).isEqualTo("08:00");
		assertThat(results.get(1).actionDay()).isEqualTo("2024-12-10");
		assertThat(results.get(1).actionTime()).isEqualTo("12:00");
		assertThat(results.get(2).actionDay()).isEqualTo("2024-12-10");
		assertThat(results.get(2).actionTime()).isEqualTo("16:00");
		assertThat(results.get(3).actionDay()).isEqualTo("2024-12-10");
		assertThat(results.get(3).actionTime()).isEqualTo("20:00");
		assertThat(results.get(4).actionDay()).isEqualTo("2024-12-11");
		assertThat(results.get(4).actionTime()).isEqualTo("00:00");
		assertThat(results.get(5).actionDay()).isEqualTo("2024-12-11");
		assertThat(results.get(5).actionTime()).isEqualTo("04:00");
		assertThat(results.get(6).actionDay()).isEqualTo("2024-12-11");
		assertThat(results.get(6).actionTime()).isEqualTo("08:00");
		assertThat(results.get(7).actionDay()).isEqualTo("2024-12-11");
		assertThat(results.get(7).actionTime()).isEqualTo("12:00");
	}
	
}
