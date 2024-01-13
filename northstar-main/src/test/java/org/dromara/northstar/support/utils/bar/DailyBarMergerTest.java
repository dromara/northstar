package org.dromara.northstar.support.utils.bar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.strategy.MergedBarListener;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DailyBarMergerTest {
	
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

	@Test
	void test() {
		DailyBarMerger merger = new DailyBarMerger(1, contract);
		MergedBarListener listener = mock(MergedBarListener.class);
		merger.addListener(listener);
		merger.onBar(genBar(LocalDate.now(), LocalTime.of(14, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.now(), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.now().plusDays(1), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		
		verify(listener, Mockito.times(2)).onMergedBar(any(Bar.class));
	}

	@Test
	void test2() {
		DailyBarMerger merger = new DailyBarMerger(2, contract);
		MergedBarListener listener = mock(MergedBarListener.class);
		merger.addListener(listener);
		merger.onBar(genBar(LocalDate.now(), LocalTime.of(14, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.now(), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		merger.onBar(genBar(LocalDate.now().plusDays(1), LocalTime.of(15, 0), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
		
		verify(listener, Mockito.times(1)).onMergedBar(any(Bar.class));
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

