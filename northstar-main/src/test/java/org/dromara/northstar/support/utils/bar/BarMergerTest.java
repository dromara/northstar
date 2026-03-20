package org.dromara.northstar.support.utils.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.gateway.playback.ticker.SimplePriceSimulation;
import org.dromara.northstar.strategy.MergedBarListener;
import org.dromara.northstar.support.utils.bar.BarTimePadding.MinuteEntry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import xyz.redtorch.pb.CoreEnum.BarCycleEnum;

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
    
    List<TimeSlot> times3 = List.of(
			TimeSlot.builder().start(LocalTime.of(21, 0)).end(LocalTime.of(23, 00)).build(),
			TimeSlot.builder().start(LocalTime.of(9, 0)).end(LocalTime.of(10, 15)).build(),
			TimeSlot.builder().start(LocalTime.of(10, 30)).end(LocalTime.of(11, 30)).build(),
			TimeSlot.builder().start(LocalTime.of(13, 30)).end(LocalTime.of(15, 0)).build()
		);
    
    Contract contract3 = Contract.builder()
    		.unifiedSymbol("rb2401@SHFE@FUTURES")
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder()
    						.timeSlots(times3)
    						.build())
    				.build())
    		.build();
    
	List<TimeSlot> times4 = List.of(
			TimeSlot.builder().start(LocalTime.of(0, 0)).end(LocalTime.of(0, 0)).build()
		);
    
    Contract contract4 = Contract.builder()
    		.unifiedSymbol("rb2401@SHFE@FUTURES")
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder().timeSlots(times4).weekendClosure(false).build())
    				.build())
    		.build();
 
    List<TimeSlot> times5 = List.of(
			TimeSlot.builder().start(LocalTime.of(6, 0)).end(LocalTime.of(5, 59)).build()
		);
    
    Contract contract5 = Contract.builder()
    		.unifiedSymbol("rb2401@SHFE@FUTURES")
    		.contractDefinition(ContractDefinition.builder()
    				.tradeTimeDef(TradeTimeDefinition.builder().timeSlots(times5).weekendClosure(false).build())
    				.build())
    		.usingStartTimeInBarTime(true)
    		.build();
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testMapping1Min(){
        BarMerger merger = new BarMerger(1, contract);
        for(Entry<MinuteEntry, LocalTime> e : merger.barTimePadding.barTimeEntryMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimePadding.barTimeEntryMap.values())).hasSize(555);
        assertThat(merger.barTimePadding.barTimeEntryMap).hasSize(555);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testMapping5Min(){
        BarMerger merger = new BarMerger(5, contract);
        for(Entry<MinuteEntry, LocalTime> e : merger.barTimePadding.barTimeEntryMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimePadding.barTimeEntryMap.values())).hasSize(111);
        assertThat(merger.barTimePadding.barTimeEntryMap).hasSize(555);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testMapping120Min(){
        BarMerger merger = new BarMerger(120, contract);
        for(Entry<MinuteEntry, LocalTime> e : merger.barTimePadding.barTimeEntryMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimePadding.barTimeEntryMap.values())).hasSize(5);
        assertThat(merger.barTimePadding.barTimeEntryMap).hasSize(555);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testMapping240Min(){
        BarMerger merger = new BarMerger(240, contract3);
        for(Entry<MinuteEntry, LocalTime> e : merger.barTimePadding.barTimeEntryMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimePadding.barTimeEntryMap.values())).hasSize(2);
        assertThat(merger.barTimePadding.barTimeEntryMap).hasSize(345);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testWholeDayMapping1Min(){
        BarMerger merger = new BarMerger(1, contract2);
        for(Entry<MinuteEntry, LocalTime> e : merger.barTimePadding.barTimeEntryMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimePadding.barTimeEntryMap.values())).hasSize(1440);
        assertThat(merger.barTimePadding.barTimeEntryMap).hasSize(1440);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
    void testWholeDayMapping60Min(){
        BarMerger merger = new BarMerger(60, contract2);
        for(Entry<MinuteEntry, LocalTime> e : merger.barTimePadding.barTimeEntryMap.entrySet()) {
        	System.out.println(String.format("%s -> %s", e.getKey(), e.getValue()));
        }
        assertThat(new HashSet(merger.barTimePadding.barTimeEntryMap.values())).hasSize(24);
        assertThat(merger.barTimePadding.barTimeEntryMap).hasSize(1440);
    }

    @Test
    void test1MinCount(){
    	BarMerger merger = new BarMerger(1, contract);
    	List<Bar> results = new ArrayList<Bar>(555);
    	MergedBarListener listener = bar -> {
    		System.out.println(String.format("actionDay:%s  actionTime: %s", bar.actionDay(), bar.actionTime()));
    		results.add(bar);
    	};
    	merger.addListener(listener);
    	for(Bar bar : sampling(LocalTime.of(21, 0))) {
    		merger.onBar(bar);
    	}
    	assertThat(results).hasSize(555);
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
    	Bar b1 = genBar(LocalDate.now(), LocalTime.of(21, 0), 5500, 4500, 5000, 5100, 10010, 10, 20200, 200, 30001, 1);
    	Bar b2 = genBar(LocalDate.now(), LocalTime.of(21, 1), 5600, 4800, 5100, 5200, 10020, 10, 20400, 200, 30002, 2);
    	Bar b3 = genBar(LocalDate.now(), LocalTime.of(21, 2), 5400, 4900, 5200, 5100, 10030, 10, 20600, 200, 30002, 0);
    	Bar b4 = genBar(LocalDate.now(), LocalTime.of(21, 3), 5300, 4600, 5300, 5150, 10040, 10, 20800, 200, 30003, 1);
    	BarMerger merger = new BarMerger(3, contract);
    	MergedBarListener listener = new MergedBarListener() {
			
			@Override
			public void onMergedBar(Bar bar) {
				assertThat(bar.highPrice()).isCloseTo(5600D, offset(1e-9));
				assertThat(bar.lowPrice()).isCloseTo(4500D, offset(1e-9));
				assertThat(bar.openPrice()).isCloseTo(5000D, offset(1e-9));
				assertThat(bar.closePrice()).isCloseTo(5150D, offset(1e-9));
				assertThat(bar.openInterest()).isEqualTo(10040);
				assertThat(bar.openInterestDelta()).isEqualTo(40);
				assertThat(bar.volume()).isEqualTo(20800);
				assertThat(bar.volumeDelta()).isEqualTo(800);
				assertThat(bar.turnover()).isCloseTo(30003D, offset(1e-9));
				assertThat(bar.turnoverDelta()).isCloseTo(4D, offset(1e-9));
			}
			
		};
		merger.addListener(listener);
		merger.onBar(b1);
		merger.onBar(b2);
		merger.onBar(b3);
		merger.onBar(b4);
    }
    
    @Test
    void test240MinMergingOnSunday() {
    	Bar b1 = genBar(contract5, LocalDate.of(2024, 12, 2), LocalTime.of(6, 0), BarCycleEnum.B_1Hr, 5500, 4500, 5000, 5100, 10010, 10, 20200, 200, 30001, 1);
    	Bar b2 = genBar(contract5, LocalDate.of(2024, 12, 2), LocalTime.of(7, 0), BarCycleEnum.B_1Hr, 5600, 4800, 5100, 5200, 10020, 10, 20400, 200, 30002, 2);
    	Bar b3 = genBar(contract5, LocalDate.of(2024, 12, 2), LocalTime.of(8, 0), BarCycleEnum.B_1Hr, 5400, 4900, 5200, 5100, 10030, 10, 20600, 200, 30002, 0);
    	Bar b4 = genBar(contract5, LocalDate.of(2024, 12, 2), LocalTime.of(9, 0), BarCycleEnum.B_1Hr, 5400, 4900, 5200, 5100, 10030, 10, 20600, 200, 30002, 0);
    	Bar b5 = genBar(contract5, LocalDate.of(2024, 12, 2), LocalTime.of(10, 0), BarCycleEnum.B_1Hr, 5400, 4900, 5200, 5100, 10030, 10, 20600, 200, 30002, 0);
    	Bar b6 = genBar(contract5, LocalDate.of(2024, 12, 2), LocalTime.of(11, 0), BarCycleEnum.B_1Hr, 5400, 4900, 5200, 5100, 10030, 10, 20600, 200, 30002, 0);
    	Bar b7 = genBar(contract5, LocalDate.of(2024, 12, 2), LocalTime.of(12, 0), BarCycleEnum.B_1Hr, 5400, 4900, 5200, 5100, 10030, 10, 20600, 200, 30002, 0);
    	Bar b8 = genBar(contract5, LocalDate.of(2024, 12, 2), LocalTime.of(13, 0), BarCycleEnum.B_1Hr, 5400, 4900, 5200, 5100, 10030, 10, 20600, 200, 30002, 0);
    	
    	BarMerger merger = new BarMerger(240, contract5);
    	List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	List.of(b1,b2,b3,b4,b5,b6,b7,b8).stream().forEach(merger::onBar);
    	
    	assertThat(results).hasSize(2);
    	assertThat(results.getFirst().actionDay()).isEqualTo("2024-12-02");
    	assertThat(results.getFirst().actionTime()).isEqualTo("06:00");
    	assertThat(results.getLast().actionDay()).isEqualTo("2024-12-02");
    	assertThat(results.getLast().actionTime()).isEqualTo("10:00");
    }
    
    @Test
    void test240MinMerging() {
    	Bar b1 = genBar(LocalDate.of(2024, 11, 22), LocalTime.of(21, 0), 5500, 4500, 5000, 5100, 10010, 10, 20200, 200, 30001, 1);
    	Bar b2 = genBar(LocalDate.of(2024, 11, 22), LocalTime.of(22, 0), 5600, 4800, 5100, 5200, 10020, 10, 20400, 200, 30002, 2);
    	Bar b3 = genBar(LocalDate.of(2024, 11, 22), LocalTime.of(23, 0), 5400, 4900, 5200, 5100, 10030, 10, 20600, 200, 30002, 0);
    	Bar b4 = genBar(LocalDate.of(2024, 11, 25), LocalTime.of(9, 0), 5300, 4600, 5300, 5150, 10040, 10, 20800, 200, 30003, 1);
    	Bar b5 = genBar(LocalDate.of(2024, 11, 25), LocalTime.of(10, 0), 5300, 4600, 5300, 5150, 10040, 10, 20800, 200, 30003, 1);
    	Bar b6 = genBar(LocalDate.of(2024, 11, 25), LocalTime.of(11, 0), 5300, 4600, 5300, 5150, 10040, 10, 20800, 200, 30003, 1);
    	Bar b7 = genBar(LocalDate.of(2024, 11, 25), LocalTime.of(14, 0), 5300, 4600, 5300, 5150, 10040, 10, 20800, 200, 30003, 1);
    	Bar b8 = genBar(LocalDate.of(2024, 11, 25), LocalTime.of(15, 0), 5300, 4600, 5300, 5150, 10040, 10, 20800, 200, 30003, 1);
    	
    	BarMerger merger = new BarMerger(240, contract3);
    	List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	List.of(b1,b2,b3,b4,b5,b6,b7,b8).stream().forEach(merger::onBar);
    	assertThat(results).hasSize(2);
    	assertThat(results.getFirst().actionDay()).isEqualTo("2024-11-25");
    	assertThat(results.getFirst().actionTime()).isEqualTo("11:15");
    	assertThat(results.getLast().actionDay()).isEqualTo("2024-11-25");
    	assertThat(results.getLast().actionTime()).isEqualTo("15:00");
    }
    
    @Test
    void test240MinMergingWithWeekend() {
    	LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 11, 20), LocalTime.of(21, 0));
    	
    	BarMerger merger = new BarMerger(240, contract4);
    	List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	for(int i=0; i<24; i++) {
    		Bar bar = genBar(contract4, ldt.toLocalDate(), ldt.toLocalTime(), BarCycleEnum.B_1Hr, 5500, 4500, 5000, 5100, 10010, 10, 20200, 200, 30001, 1);
    		merger.onBar(bar);
    		ldt = ldt.plusHours(1);
    	}
    	assertThat(results).hasSize(6);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-11-21");
    	assertThat(results.get(0).actionTime()).isEqualTo("00:00");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-11-21");
    	assertThat(results.get(1).actionTime()).isEqualTo("04:00");
    	assertThat(results.get(2).actionDay()).isEqualTo("2024-11-21");
    	assertThat(results.get(2).actionTime()).isEqualTo("08:00");
    	assertThat(results.get(3).actionDay()).isEqualTo("2024-11-21");
    	assertThat(results.get(3).actionTime()).isEqualTo("12:00");
    }
    
    @Test
    void test60MinMerging() {
    	LocalDateTime ldt = LocalDateTime.of(LocalDate.of(2024, 11, 22), LocalTime.of(21, 0));
    	
    	BarMerger merger = new BarMerger(60, contract3);
    	List<Bar> results = new LinkedList<>();
    	merger.addListener(bar -> results.add(bar));
    	for(int i=0; i<5; i++) {
    		Bar bar = genBar(ldt.toLocalDate(), ldt.toLocalTime(), 5500, 4500, 5000, 5100, 10010, 10, 20200, 200, 30001, 1);
    		merger.onBar(bar);
    		ldt = ldt.plusMinutes(30);
    	}
    	assertThat(results).hasSize(2);
    	assertThat(results.get(0).actionDay()).isEqualTo("2024-11-22");
    	assertThat(results.get(0).actionTime()).isEqualTo("22:00");
    	assertThat(results.get(1).actionDay()).isEqualTo("2024-11-22");
    	assertThat(results.get(1).actionTime()).isEqualTo("23:00");
    }
    
    @Test
    void test1MinTicking() {
    	SimplePriceSimulation sps = new SimplePriceSimulation();
    	BarUtils utils = new BarUtils(5000, contract, BarCycleEnum.B_1Min);
    	Bar b0 = utils.genBar(LocalDate.now(), LocalTime.of(21, 0));
    	Bar b1 = utils.genBar(LocalDate.now(), LocalTime.of(21, 1));
    	Bar b2 = utils.genBar(LocalDate.now(), LocalTime.of(21, 2));
    	List<Bar> result = new ArrayList<>();
    	BarMerger merger = new BarMerger(1, contract);
    	MergedBarListener listener = new MergedBarListener() {
			
			@Override
			public void onMergedBar(Bar bar) {
				result.add(bar);
			}
			
		};
		merger.addListener(listener);
		
    	List.of(b0, b1, b2).forEach(bar -> {
    		sps.generateFrom(bar).stream()
    			.map(tickEntry -> {
    				LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(tickEntry.timestamp()), ZoneId.systemDefault());
    				Tick tick = Tick.builder()
    						.preClosePrice(bar.preClosePrice())		// 采用分钟K线的模糊值
    						.preOpenInterest(bar.preOpenInterest())	// 采用分钟K线的模糊值
    						.preSettlePrice(bar.preSettlePrice())	// 采用分钟K线的模糊值
    						.highPrice(bar.highPrice())				// 采用分钟K线的模糊值
    						.lowPrice(bar.lowPrice())				// 采用分钟K线的模糊值
    						.contract(bar.contract())
    						.gatewayId("unittest")
    						.tradingDay(bar.tradingDay())
    						.actionDay(bar.actionDay())
    						.actionTime(ldt.toLocalTime())
    						.actionTimestamp(tickEntry.timestamp())
    						.type(TickType.PLAYBACK_TICK)
    						.lastPrice(tickEntry.price())
    						.askPrice(List.of(tickEntry.askPrice0())) // 仅模拟卖一价
    						.bidPrice(List.of(tickEntry.bidPrice0())) // 仅模拟买一价
    						.askVolume(List.of(ThreadLocalRandom.current().nextInt(10,500))) // 随机模拟卖一量
    						.bidVolume(List.of(ThreadLocalRandom.current().nextInt(10,500))) // 随机模拟买一量
    						.channelType(ChannelType.PLAYBACK)
    						.volume(bar.volume())								// 采用分钟K线的模糊值
    						.volumeDelta(Math.max(1, tickEntry.volume()))		// 采用模拟随机值
    						.openInterest(bar.openInterest())					// 采用分钟K线的模糊值
    						.openInterestDelta(tickEntry.openInterestDelta())	// 采用模拟随机值
    						.turnoverDelta(bar.turnoverDelta())					// 采用分钟K线的模糊值
    						.turnover(bar.turnover())							// 采用分钟K线的模糊值
    						.build();
    				return tick;
    			})
    			.forEach(tick -> merger.onTick(tick));
    		merger.onBar(bar);
    	});;
		
		assertThat(result).hasSize(10);
		assertThat(result.get(0).unstable()).isTrue();
		assertThat(result.get(1).unstable()).isTrue();
		assertThat(result.get(2).unstable()).isTrue();
		assertThat(result.get(3).unstable()).isTrue();
		assertThat(result.get(4).unstable()).isFalse();
		assertThat(result.get(5).unstable()).isTrue();
		assertThat(result.get(6).unstable()).isTrue();
		assertThat(result.get(7).unstable()).isTrue();
		assertThat(result.get(8).unstable()).isTrue();
		assertThat(result.get(9).unstable()).isFalse();
		
		assertThat(result.get(0).actionTime()).isEqualTo(LocalTime.of(21, 1));
		assertThat(result.get(1).actionTime()).isEqualTo(LocalTime.of(21, 1));
		assertThat(result.get(2).actionTime()).isEqualTo(LocalTime.of(21, 1));
		assertThat(result.get(3).actionTime()).isEqualTo(LocalTime.of(21, 1));
		assertThat(result.get(4).actionTime()).isEqualTo(LocalTime.of(21, 1));
		assertThat(result.get(5).actionTime()).isEqualTo(LocalTime.of(21, 2));
		assertThat(result.get(6).actionTime()).isEqualTo(LocalTime.of(21, 2));
		assertThat(result.get(7).actionTime()).isEqualTo(LocalTime.of(21, 2));
		assertThat(result.get(8).actionTime()).isEqualTo(LocalTime.of(21, 2));
		assertThat(result.get(9).actionTime()).isEqualTo(LocalTime.of(21, 2));
		
		assertThat(result.get(4).actionTimestamp()).isEqualTo(b1.actionTimestamp());
		assertThat(result.get(4).openPrice()).isEqualTo(b1.openPrice());
		assertThat(result.get(4).closePrice()).isEqualTo(b1.closePrice());
		assertThat(result.get(4).highPrice()).isEqualTo(Math.max(b0.highPrice(), b1.highPrice()));
		assertThat(result.get(4).lowPrice()).isEqualTo(Math.min(b0.lowPrice(), b1.lowPrice()));
		
		assertThat(result.get(9).actionTimestamp()).isEqualTo(b2.actionTimestamp());
		assertThat(result.get(9).openPrice()).isEqualTo(b2.openPrice());
		assertThat(result.get(9).closePrice()).isEqualTo(b2.closePrice());
		assertThat(result.get(9).highPrice()).isEqualTo(b2.highPrice());
		assertThat(result.get(9).lowPrice()).isEqualTo(b2.lowPrice());
    }
    
    @Test
    void test3MinTicking() {
    	Tick t1 = genTick(LocalDate.now(), LocalTime.of(21, 0, 10), 5500, 4500, 5000, 5055, 10000, 10, 20000, 200, 30001, 300);
    	Tick t2 = genTick(LocalDate.now(), LocalTime.of(21, 1, 10), 5500, 4500, 5000, 5066, 10000, 10, 20000, 200, 30001, 300);
    	Bar b1 = genBar(LocalDate.now(), LocalTime.of(21, 1, 0), 5500, 4500, 5000, 5100, 10000, 10, 20000, 200, 30001, 300);
    	List<Bar> result = new ArrayList<>();
    	BarMerger merger = new BarMerger(3, contract);
    	MergedBarListener listener = new MergedBarListener() {
			
			@Override
			public void onMergedBar(Bar bar) {
				result.add(bar);
			}
			
		};
		merger.addListener(listener);
		merger.onTick(t1);
		merger.onBar(b1);
		merger.onTick(t2);
		
		assertThat(result).hasSize(2);
		assertThat(result.get(0).unstable()).isTrue();
		assertThat(result.get(1).unstable()).isTrue();
		
		assertThat(result.get(0).actionTime()).isEqualTo(LocalTime.of(21, 3));
		assertThat(result.get(1).actionTime()).isEqualTo(LocalTime.of(21, 3));
    }
    
    List<Bar> sampling(LocalTime start){
    	List<Bar> result = new ArrayList<>();
    	LocalDateTime dtStart = LocalDateTime.of(LocalDate.of(2024, 12, 12), start);
    	LocalDateTime dtEnd = LocalDateTime.of(LocalDate.of(2024, 12, 13), start);
    	LocalDateTime t = dtStart;
    	while(!t.isAfter(dtEnd)) {
    		result.add(genBar(t.toLocalDate(), t.toLocalTime(), 5500, 4500, 5000, 5100, 10000, 10, 20000, 200, 30001, 300));
    		t = t.plusMinutes(1);
    	}
    	return result;
    }
    
    Tick genTick(LocalDate date, LocalTime time, double h, double l, double o, double c, long op, int opDelta, long vol, int volDelta, double tr, double trDelta) {
    	return Tick.builder()
    			.contract(contract)
				.actionDay(date)
				.actionTime(time)
				.actionTimestamp(LocalDateTime.of(date, time).toInstant(ZoneOffset.ofHours(8)).toEpochMilli())
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
    
    Bar genBar(Contract contract, LocalDate date, LocalTime time, BarCycleEnum period, double h, double l, double o, double c, long op, int opDelta, long vol, int volDelta, double tr, double trDelta){
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
				.barPeriod(period)
				.build();
    }
}
