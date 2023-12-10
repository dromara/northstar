package org.dromara.northstar.support.utils.bar;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.strategy.MergedBarListener;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BarMergerTest {
	
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
    		.unifiedSymbol("BTC")
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(List.of(
    									TimeSlot.builder().start(LocalTime.of(0, 0)).end(LocalTime.of(0, 0)).build()
    								))
    						.build())
    				.build())
    		.build();

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testMapping1Min(){
        BarMerger merger = new BarMerger(1, contract);
        for(Entry<LocalTime, LocalTime> e : merger.barTimeMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimeMap.values())).hasSize(555);
        assertThat(merger.barTimeMap).hasSize(559);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testMapping5Min(){
        BarMerger merger = new BarMerger(5, contract);
        for(Entry<LocalTime, LocalTime> e : merger.barTimeMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimeMap.values())).hasSize(111);
        assertThat(merger.barTimeMap).hasSize(559);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testMapping120Min(){
        BarMerger merger = new BarMerger(120, contract);
        for(Entry<LocalTime, LocalTime> e : merger.barTimeMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimeMap.values())).hasSize(5);
        assertThat(merger.barTimeMap).hasSize(559);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testWholeDayMapping1Min(){
        BarMerger merger = new BarMerger(1, contract2);
        for(Entry<LocalTime, LocalTime> e : merger.barTimeMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimeMap.values())).hasSize(1440);
        assertThat(merger.barTimeMap).hasSize(1440);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testWholeDayMapping60Min(){
        BarMerger merger = new BarMerger(60, contract2);
        for(Entry<LocalTime, LocalTime> e : merger.barTimeMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimeMap.values())).hasSize(24);
        assertThat(merger.barTimeMap).hasSize(1440);
    }

    @Test
    void test1MinCount(){
    	BarMerger merger = new BarMerger(1, contract);
    	MergedBarListener listener = mock(MergedBarListener.class);
    	merger.addListener(listener);
    	for(Bar bar : sampling(LocalTime.of(21, 0))) {
    		merger.onBar(bar);
    	}
    	verify(listener, Mockito.times(555)).onMergedBar(any(Bar.class));
    }

    @Test
    void test3MinCount(){
    	BarMerger merger = new BarMerger(3, contract);
    	MergedBarListener listener = mock(MergedBarListener.class);
    	merger.addListener(listener);
    	for(Bar bar : sampling(LocalTime.of(21, 0))) {
    		merger.onBar(bar);
    	}
    	verify(listener, Mockito.times(185)).onMergedBar(any(Bar.class));
    }

    @Test
    void test1HourCount(){
    	BarMerger merger = new BarMerger(60, contract);
    	MergedBarListener listener = mock(MergedBarListener.class);
    	merger.addListener(listener);
    	for(Bar bar : sampling(LocalTime.of(21, 0))) {
    		merger.onBar(bar);
    	}
    	verify(listener, Mockito.times(10)).onMergedBar(any(Bar.class));
    }
    
    @Test
    void test3MinMerging() {
    	Bar b1 = genBar(LocalDate.now(), LocalTime.of(21, 0), 5500, 4500, 5000, 5100, 10000, 10, 20000, 200, 30001, 300);
    	Bar b2 = genBar(LocalDate.now(), LocalTime.of(21, 1), 5600, 4800, 5100, 5200, 10001, 10, 20002, 200, 30002, 300);
    	Bar b3 = genBar(LocalDate.now(), LocalTime.of(21, 2), 5400, 4900, 5200, 5100, 10002, 10, 20003, 200, 30002, 300);
    	Bar b4 = genBar(LocalDate.now(), LocalTime.of(21, 3), 5300, 4600, 5300, 5150, 10003, 10, 20004, 200, 30003, 300);
    	BarMerger merger = new BarMerger(3, contract);
    	MergedBarListener listener = new MergedBarListener() {
			
			@Override
			public void onMergedBar(Bar bar) {
				assertThat(bar.highPrice()).isCloseTo(5600D, offset(1e-9));
				assertThat(bar.lowPrice()).isCloseTo(4500D, offset(1e-9));
				assertThat(bar.openPrice()).isCloseTo(5000D, offset(1e-9));
				assertThat(bar.closePrice()).isCloseTo(5150D, offset(1e-9));
				assertThat(bar.openInterest()).isCloseTo(10003D, offset(1e-9));
				assertThat(bar.openInterestDelta()).isCloseTo(40D, offset(1e-9));
				assertThat(bar.volume()).isEqualTo(20004);
				assertThat(bar.volumeDelta()).isEqualTo(800);
				assertThat(bar.turnover()).isCloseTo(30003D, offset(1e-9));
				assertThat(bar.turnoverDelta()).isCloseTo(1200D, offset(1e-9));
			}
			
		};
		merger.addListener(listener);
		merger.onBar(b1);
		merger.onBar(b2);
		merger.onBar(b3);
		merger.onBar(b4);
		
    }
    
    List<Bar> sampling(LocalTime start){
    	List<Bar> result = new ArrayList<>();
    	LocalDateTime dtStart = LocalDateTime.of(LocalDate.now(), start);
    	LocalDateTime dtEnd = LocalDateTime.of(LocalDate.now().plusDays(1), start);
    	LocalDateTime t = dtStart;
    	while(!t.isAfter(dtEnd)) {
    		result.add(genBar(t.toLocalDate(), t.toLocalTime(), 5500, 4500, 5000, 5100, 10000, 10, 20000, 200, 30001, 300));
    		t = t.plusMinutes(1);
    	}
    	return result;
    }
    
    Bar genBar(LocalDate date, LocalTime time, double h, double l, double o, double c, double op, double opDelta, long vol, long volDelta, double tr, double trDelta){
    	return Bar.builder()
    			.contract(contract)
				.actionDay(date)
				.actionTime(time)
				.actionTimestamp(LocalDateTime.of(date, time).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
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
