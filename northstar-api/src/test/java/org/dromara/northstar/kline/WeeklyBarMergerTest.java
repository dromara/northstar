package org.dromara.northstar.kline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.strategy.MergedBarListener;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WeeklyBarMergerTest {

	List<TimeSlot> times = List.of(
			TimeSlot.builder().start(LocalTime.of(21, 0)).end(LocalTime.of(2, 30)).build(),
			TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(10, 15)).build(),
			TimeSlot.builder().start(LocalTime.of(10, 30)).end(LocalTime.of(11, 30)).build(),
			TimeSlot.builder().start(LocalTime.of(13, 30)).end(LocalTime.of(15, 0)).build()
		);

    Contract contract = Contract.builder()
    		.unifiedSymbol("rb2401@SHFE@FUTURES")
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(times)
    						.build())
    				.build())
    		.build();
    
    Contract contract2 = Contract.builder()
    		.unifiedSymbol("rb2401@SHFE@FUTURES")
    		.activeInWeekend(true)
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(times)
    						.build())
    				.build())
    		.build();
    
    Contract contractStart = Contract.builder()
    		.unifiedSymbol("rb2401@SHFE@FUTURES")
    		.usingStartTimeInBarTime(true)
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(times)
    						.build())
    				.build())
    		.build();

	@Test
	void test() {
		WeeklyBarMerger merger = new WeeklyBarMerger(contract);
		MergedBarListener listener = mock(MergedBarListener.class);
		merger.addListener(listener);
		merger.onBar(genBar(LocalDate.of(2024, 9, 10), LocalTime.of(14, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.of(2024, 9, 13), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		verify(listener, Mockito.times(1)).onMergedBar(any(Bar.class));
		merger.onBar(genBar(LocalDate.of(2024, 9, 15), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		verify(listener, Mockito.times(1)).onMergedBar(any(Bar.class));
	}

	@Test
	void test2() {
		WeeklyBarMerger merger = new WeeklyBarMerger(contract2);
		MergedBarListener listener = mock(MergedBarListener.class);
		merger.addListener(listener);
		merger.onBar(genBar(LocalDate.of(2024, 9, 10), LocalTime.of(14, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.of(2024, 9, 13), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.of(2024, 9, 15), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		verify(listener, Mockito.times(1)).onMergedBar(any(Bar.class));
	}
	
	@Test
	void testTick() {
		WeeklyBarMerger merger = new WeeklyBarMerger(contract);
		List<Bar> result = new ArrayList<>();
		MergedBarListener listener = bar -> {
			result.add(bar);
		};
		merger.addListener(listener);
		merger.onBar(genBar(LocalDate.of(2024, 9, 10), LocalTime.of(14, 0), 100, 90, 95, 97, 0, 0, 0, 0, 0, 0));
		merger.onTick(genTick(LocalDate.of(2024, 9, 10), LocalTime.of(15, 0), 110, 80, 100, 99, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.of(2024, 9, 13), LocalTime.of(15, 0), 105, 100, 98, 99, 0, 0, 0, 0, 0, 0));
		merger.onTick(genTick(LocalDate.of(2024, 9, 19), LocalTime.of(15, 0), 120, 85, 115, 116, 0, 0, 0, 0, 0, 0));
		
		assertThat(result).hasSize(3);
		assertThat(result.get(0).interval()).isEqualTo(Interval.WK1);
		assertThat(result.get(1).interval()).isEqualTo(Interval.WK1);
		assertThat(result.get(0).unstable()).isTrue();
		assertThat(result.get(1).unstable()).isFalse();
		
		assertThat(result.get(1).actionDay()).isEqualTo(LocalDate.of(2024, 9, 13));
		assertThat(result.get(1).actionTime()).isEqualTo(LocalTime.of(15, 0));
		assertThat(result.get(1).openPrice()).isCloseTo(95D, offset(1e-1));
		assertThat(result.get(1).closePrice()).isCloseTo(99D, offset(1e-1));
		assertThat(result.get(1).highPrice()).isCloseTo(105D, offset(1e-1));
		assertThat(result.get(1).lowPrice()).isCloseTo(90D, offset(1e-1));
		
		assertThat(result.get(2).actionDay()).isEqualTo(LocalDate.of(2024, 9, 20));
		assertThat(result.get(2).actionTime()).isEqualTo(LocalTime.of(15, 0));
		assertThat(result.get(2).openPrice()).isCloseTo(115D, offset(1e-1));
		assertThat(result.get(2).closePrice()).isCloseTo(116D, offset(1e-1));
		assertThat(result.get(2).highPrice()).isCloseTo(120D, offset(1e-1));
		assertThat(result.get(2).lowPrice()).isCloseTo(85D, offset(1e-1));
	}
	
	@Test
	void testCloseTimeWeeklyBarTime() {
		WeeklyBarMerger merger = new WeeklyBarMerger(contract);
		List<Bar> results = new ArrayList<>();
		merger.addListener(results::add);
		merger.onBar(genBar(LocalDate.of(2024, 9, 9), LocalTime.of(21, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.of(2024, 9, 13), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		assertThat(results).hasSize(1);
		Bar bar = results.get(0);
		assertThat(bar.actionDay()).isEqualTo(LocalDate.of(2024, 9, 13));
		assertThat(bar.actionTime()).isEqualTo(LocalTime.of(15, 0));
		assertThat(bar.interval()).isEqualTo(Interval.WK1);
		assertThat(bar.unstable()).isFalse();
	}
	
	@Test
	void testOpenTimeWeeklyBarTime() {
		WeeklyBarMerger merger = new WeeklyBarMerger(contractStart);
		List<Bar> results = new ArrayList<>();
		merger.addListener(results::add);
		merger.onBar(genBar(LocalDate.of(2024, 9, 9), LocalTime.of(21, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.of(2024, 9, 13), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		assertThat(results).hasSize(1);
		Bar bar = results.get(0);
		assertThat(bar.actionDay()).isEqualTo(LocalDate.of(2024, 9, 9));
		assertThat(bar.actionTime()).isEqualTo(LocalTime.of(21, 0));
		assertThat(bar.interval()).isEqualTo(Interval.WK1);
		assertThat(bar.unstable()).isFalse();
	}
	
	@Test
	void testTickOnlyCloseTimeWeekly() {
		WeeklyBarMerger merger = new WeeklyBarMerger(contract);
		List<Bar> results = new ArrayList<>();
		merger.addListener(results::add);
		merger.onTick(genTick(contract, LocalDate.of(2024, 9, 10), LocalTime.of(14, 30), 110, 80, 100, 99, 0, 0, 0, 0, 0, 0));
		assertThat(results).hasSize(1);
		Bar bar = results.get(0);
		assertThat(bar.unstable()).isTrue();
		assertThat(bar.actionDay()).isEqualTo(LocalDate.of(2024, 9, 13));
		assertThat(bar.actionTime()).isEqualTo(LocalTime.of(15, 0));
		assertThat(bar.interval()).isEqualTo(Interval.WK1);
	}
	
	@Test
	void testTickOnlyOpenTimeWeekly() {
		WeeklyBarMerger merger = new WeeklyBarMerger(contractStart);
		List<Bar> results = new ArrayList<>();
		merger.addListener(results::add);
		merger.onTick(genTick(contractStart, LocalDate.of(2024, 9, 10), LocalTime.of(14, 30), 110, 80, 100, 99, 0, 0, 0, 0, 0, 0));
		assertThat(results).hasSize(1);
		Bar bar = results.get(0);
		assertThat(bar.unstable()).isTrue();
		assertThat(bar.actionDay()).isEqualTo(LocalDate.of(2024, 9, 9));
		assertThat(bar.actionTime()).isEqualTo(LocalTime.of(21, 0));
		assertThat(bar.interval()).isEqualTo(Interval.WK1);
	}
	
	Tick genTick(LocalDate date, LocalTime time, double h, double l, double o, double c, long op, int opDelta, long vol, int volDelta, double tr, double trDelta) {
    	return Tick.builder()
    			.contract(contract)
				.actionDay(date)
				.actionTime(time)
				.actionTimestamp(LocalDateTime.of(date, time).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
				.tradingDay(date)
				.openPrice(o)
				.lastPrice(c)
				.highPrice(h)
				.lowPrice(l)
				.openInterest(op)
				.openInterestDelta(opDelta)
				.volume(vol)
				.volumeDelta(volDelta)
				.turnover(tr)
				.turnoverDelta(trDelta)
    			.build();
	}
	
	Tick genTick(Contract contract, LocalDate date, LocalTime time, double h, double l, double o, double c, long op, int opDelta, long vol, int volDelta, double tr, double trDelta) {
        return Tick.builder()
                .contract(contract)
                .actionDay(date)
                .actionTime(time)
                .actionTimestamp(LocalDateTime.of(date, time).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
                .tradingDay(date)
                .openPrice(o)
                .lastPrice(c)
                .highPrice(h)
                .lowPrice(l)
                .openInterest(op)
                .openInterestDelta(opDelta)
                .volume(vol)
                .volumeDelta(volDelta)
                .turnover(tr)
                .turnoverDelta(trDelta)
                .build();
    }
	
	Bar genBar(LocalDate date, LocalTime time, double h, double l, double o, double c, long op, int opDelta, long vol, int volDelta, double tr, double trDelta){
	    	return Bar.builder()
	    			.contract(contract)
					.actionDay(date)
					.actionTime(time)
					.actionTimestamp(LocalDateTime.of(date, time).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
					.tradingDay(date)
					.openPrice(o)
					.closePrice(c)
					.highPrice(h)
					.lowPrice(l)
					.openInterest(op)
					.openInterestDelta(opDelta)
					.volume(vol)
					.volumeDelta(volDelta)
					.turnover(tr)
					.turnoverDelta(trDelta)
					.build();
	 }
}
