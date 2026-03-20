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
import org.dromara.northstar.common.utils.DateTimeUtils;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.BarCycleEnum;

class BarMergerCnFuture4HrTest {
	
	List<TimeSlot> times = List.of(
			TimeSlot.builder().start(LocalTime.of(21, 0)).end(LocalTime.of(2, 30)).build(),
			TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(10, 15)).build(),
			TimeSlot.builder().start(LocalTime.of(10, 30)).end(LocalTime.of(11, 30)).build(),
			TimeSlot.builder().start(LocalTime.of(13, 30)).end(LocalTime.of(15, 0)).build()
		);
	
    Contract contract = Contract.builder()
    		.unifiedSymbol("au2412@SHFE@FUTURES")
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(times)
    						.build())
    				.build())
    		.build();
    
    List<TimeSlot> times2 = List.of(
			TimeSlot.builder().start(LocalTime.of(21, 0)).end(LocalTime.of(23, 00)).build(),
			TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(10, 15)).build(),
			TimeSlot.builder().start(LocalTime.of(10, 30)).end(LocalTime.of(11, 30)).build(),
			TimeSlot.builder().start(LocalTime.of(13, 30)).end(LocalTime.of(15, 0)).build()
		);
	
    Contract contract2 = Contract.builder()
    		.unifiedSymbol("rb2412@SHFE@FUTURES")
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(times2)
    						.build())
    				.build())
    		.build();
    
    List<TimeSlot> times3 = List.of(
			TimeSlot.builder().start(LocalTime.of(21, 0)).end(LocalTime.of(1, 00)).build(),
			TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(10, 15)).build(),
			TimeSlot.builder().start(LocalTime.of(10, 30)).end(LocalTime.of(11, 30)).build(),
			TimeSlot.builder().start(LocalTime.of(13, 30)).end(LocalTime.of(15, 0)).build()
		);
	
    Contract contract3 = Contract.builder()
    		.unifiedSymbol("au2412@SHFE@FUTURES")
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(times3)
    						.build())
    				.build())
    		.build();
    
    List<TimeSlot> times4 = List.of(
			TimeSlot.builder().start(DateTimeUtils.fromCacheTime(9, 30)).end(DateTimeUtils.fromCacheTime(11, 30)).build(),
			TimeSlot.builder().start(DateTimeUtils.fromCacheTime(13, 00)).end(DateTimeUtils.fromCacheTime(15, 15)).build()
		);
    
    Contract contract4 = Contract.builder()
    		.unifiedSymbol("TF0000@CFFEX@FUTURES")
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder().timeSlots(times4).build())
    				.build())
    		.build();
    
	@Test
	void test4HrNormal1DayC1() {
		BarUtils utils = new BarUtils(5000, contract, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 10), LocalTime.of(21, 0));
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
		for(int i=0; i<24; i++) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusHours(1);
    	}
		
		assertThat(results).hasSize(3);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-12-11");
    	assertThat(results.get(0).actionTime()).isEqualTo("01:00");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-11");
    	assertThat(results.get(1).actionTime()).isEqualTo("13:45");
    	assertThat(results.get(2).actionDay()).isEqualTo("2024-12-11");
    	assertThat(results.get(2).actionTime()).isEqualTo("15:00");
	}
	
	@Test
	void test4HrNormal1DayC2() {
		BarUtils utils = new BarUtils(5000, contract2, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract2);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 10), LocalTime.of(21, 0));
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
		for(int i=0; i<24; i++) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusHours(1);
    	}
		
		assertThat(results).hasSize(2);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-12-11");
    	assertThat(results.get(0).actionTime()).isEqualTo("11:15");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-11");
    	assertThat(results.get(1).actionTime()).isEqualTo("15:00");
	}
	
	@Test
	void test4HrNormal1DayC3() {
		BarUtils utils = new BarUtils(5000, contract3, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract3);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 10), LocalTime.of(21, 0));
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
		for(int i=0; i<24; i++) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusHours(1);
    	}
		
		assertThat(results).hasSize(2);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-12-11");
    	assertThat(results.get(0).actionTime()).isEqualTo("01:00");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-11");
    	assertThat(results.get(1).actionTime()).isEqualTo("15:00");
	}

	@Test
	void test4HrNormal1DayCrossWeekendC1() {
		BarUtils utils = new BarUtils(5000, contract, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 6), LocalTime.of(21, 0));
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	while(!ldt.isAfter(LocalDateTime.of(LocalDate.of(2024, 12, 7), LocalTime.of(2, 30)))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		ldt = LocalDateTime.of(LocalDate.of(2024, 12, 9), LocalTime.of(9, 0));
		while(!ldt.toLocalTime().isAfter(LocalTime.of(15, 0))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		assertThat(results).hasSize(3);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-12-07");
    	assertThat(results.get(0).actionTime()).isEqualTo("01:00");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(1).actionTime()).isEqualTo("13:45");
    	assertThat(results.get(2).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(2).actionTime()).isEqualTo("15:00");
	}
	
	@Test
	void test4HrNormal2DayCrossWeekendC1() {
		BarUtils utils = new BarUtils(5000, contract, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 5), LocalTime.of(21, 0));
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	while(!ldt.isAfter(LocalDateTime.of(LocalDate.of(2024, 12, 7), LocalTime.of(2, 30)))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		ldt = LocalDateTime.of(LocalDate.of(2024, 12, 9), LocalTime.of(9, 0));
		while(!ldt.toLocalTime().isAfter(LocalTime.of(15, 0))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		assertThat(results).hasSize(6);
		assertThat(results.get(0).actionDay()).isEqualTo("2024-12-06");
    	assertThat(results.get(0).actionTime()).isEqualTo("01:00");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-06");
    	assertThat(results.get(1).actionTime()).isEqualTo("13:45");
    	assertThat(results.get(2).actionDay()).isEqualTo("2024-12-06");
    	assertThat(results.get(2).actionTime()).isEqualTo("15:00");
    	assertThat(results.get(3).actionDay()).isEqualTo("2024-12-07");
    	assertThat(results.get(3).actionTime()).isEqualTo("01:00");
    	assertThat(results.get(4).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(4).actionTime()).isEqualTo("13:45");
    	assertThat(results.get(5).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(5).actionTime()).isEqualTo("15:00");
	}
	
	@Test
	void test4HrNormal1DayCrossWeekendC2() {
		BarUtils utils = new BarUtils(5000, contract2, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract2);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 6), LocalTime.of(21, 0));
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	while(!ldt.isAfter(LocalDateTime.of(LocalDate.of(2024, 12, 7), LocalTime.of(2, 30)))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		ldt = LocalDateTime.of(LocalDate.of(2024, 12, 9), LocalTime.of(9, 0));
		while(!ldt.toLocalTime().isAfter(LocalTime.of(15, 0))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		assertThat(results).hasSize(2);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(0).actionTime()).isEqualTo("11:15");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(1).actionTime()).isEqualTo("15:00");	
    }
	
	@Test
	void test4HrNormal2DayCrossWeekendC2() {
		BarUtils utils = new BarUtils(5000, contract2, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract2);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 5), LocalTime.of(21, 0));
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	while(!ldt.isAfter(LocalDateTime.of(LocalDate.of(2024, 12, 7), LocalTime.of(2, 30)))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		ldt = LocalDateTime.of(LocalDate.of(2024, 12, 9), LocalTime.of(9, 0));
		while(!ldt.toLocalTime().isAfter(LocalTime.of(15, 0))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		assertThat(results).hasSize(4);
		assertThat(results.get(0).actionDay()).isEqualTo("2024-12-06");
    	assertThat(results.get(0).actionTime()).isEqualTo("11:15");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-06");
    	assertThat(results.get(1).actionTime()).isEqualTo("15:00");	
    	assertThat(results.get(2).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(2).actionTime()).isEqualTo("11:15");
    	assertThat(results.get(3).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(3).actionTime()).isEqualTo("15:00");	
    }
	
	@Test
	void test4HrNormal1DayCrossWeekendC3() {
		BarUtils utils = new BarUtils(5000, contract3, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract3);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 6), LocalTime.of(21, 0));
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	while(!ldt.isAfter(LocalDateTime.of(LocalDate.of(2024, 12, 7), LocalTime.of(2, 30)))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		ldt = LocalDateTime.of(LocalDate.of(2024, 12, 9), LocalTime.of(9, 0));
		while(!ldt.toLocalTime().isAfter(LocalTime.of(15, 0))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		assertThat(results).hasSize(2);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-12-07");
    	assertThat(results.get(0).actionTime()).isEqualTo("01:00");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(1).actionTime()).isEqualTo("15:00");
	}
	
	@Test
	void test4HrNormal2DayCrossWeekendC3() {
		BarUtils utils = new BarUtils(5000, contract3, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract3);
		LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 5), LocalTime.of(21, 0));
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	while(!ldt.isAfter(LocalDateTime.of(LocalDate.of(2024, 12, 7), LocalTime.of(2, 30)))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		ldt = LocalDateTime.of(LocalDate.of(2024, 12, 9), LocalTime.of(9, 0));
		while(!ldt.toLocalTime().isAfter(LocalTime.of(15, 0))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		assertThat(results).hasSize(4);
		assertThat(results.get(0).actionDay()).isEqualTo("2024-12-06");
    	assertThat(results.get(0).actionTime()).isEqualTo("01:00");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-06");
    	assertThat(results.get(1).actionTime()).isEqualTo("15:00");
    	assertThat(results.get(2).actionDay()).isEqualTo("2024-12-07");
    	assertThat(results.get(2).actionTime()).isEqualTo("01:00");
    	assertThat(results.get(3).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(3).actionTime()).isEqualTo("15:00");
	}
	
	@Test
	void test4HrNormal1DayCrossHolidayC1() {
		BarUtils utils = new BarUtils(5000, contract, BarCycleEnum.B_1Hr);
		BarMerger merger = new BarMerger(240, contract);
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
		
    	LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 9), LocalTime.of(9, 0));
		while(!ldt.toLocalTime().isAfter(LocalTime.of(15, 0))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(60);
    	}
		
		assertThat(results).hasSize(2);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(0).actionTime()).isEqualTo("13:45");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(1).actionTime()).isEqualTo("15:00");
	}
	
	@Test
	void test4HrNormalWeekC4() {
		BarUtils utils = new BarUtils(5000, contract4, BarCycleEnum.B_15Min);
		BarMerger merger = new BarMerger(240, contract4);
		List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
		
    	LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 12, 9), LocalTime.of(9, 30));
		while(!ldt.isAfter(LocalDateTime.of(LocalDate.of(2024, 12, 13), LocalTime.of(16, 0)))) {
    		Bar bar = utils.genBar(ldt.toLocalDate(), ldt.toLocalTime());
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(15);
    	}
		
		for(Bar b : results) {
			System.out.println(String.format("%s %s", b.actionDay(), b.actionTime()));
		}
		assertThat(results).hasSize(10);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(0).actionTime()).isEqualTo("15:00");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-12-09");
    	assertThat(results.get(1).actionTime()).isEqualTo("15:15");
    	assertThat(results.get(8).actionDay()).isEqualTo("2024-12-13");
    	assertThat(results.get(8).actionTime()).isEqualTo("15:00");
    	assertThat(results.get(9).actionDay()).isEqualTo("2024-12-13");
    	assertThat(results.get(9).actionTime()).isEqualTo("15:15");
	}
}
