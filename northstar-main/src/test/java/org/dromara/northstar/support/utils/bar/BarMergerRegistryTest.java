package org.dromara.northstar.support.utils.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.module.ModuleContext;
import org.dromara.northstar.strategy.MergedBarListener;
import org.junit.jupiter.api.Test;

class BarMergerRegistryTest {
	
	BarMergerRegistry registry = new BarMergerRegistry(mock(ModuleContext.class));
	
	ContractDefinition cd = ContractDefinition.builder()
			.tradeTimeDef(TradeTimeDefinition.builder()
					.timeSlots(List.of(TimeSlot.builder().start(LocalTime.of(0, 0)).end(LocalTime.of(0, 0)).build()))
					.build())
			.build();

	Contract contract = Contract.builder().unifiedSymbol("rb2201").contractDefinition(cd).build();
	
	MergedBarListener listener1 = mock(MergedBarListener.class);
	MergedBarListener listener2 = mock(MergedBarListener.class);
	MergedBarListener listener3 = mock(MergedBarListener.class);
	
	@Test
	void test() {
		registry.addListener(contract, Interval.M5, listener1);
		registry.addListener(contract, Interval.M5, listener2);
		
		assertThat(registry.mergerMap).hasSize(1);
		
		registry.addListener(contract, Interval.M10, listener3);
		assertThat(registry.mergerMap).hasSize(2);
	}

	@Test
	void testBar() {
		registry.addListener(contract, Interval.M5, listener1);
		registry.addListener(contract, Interval.M10, listener3);
		
		Bar bar = Bar.builder().contract(contract).actionDay(LocalDate.now()).actionTime(LocalTime.now()).build();
		AtomicInteger cnt = new AtomicInteger();
		
		registry.onBar(bar);
		registry.mergerMap.values().forEach(merger -> {
			cnt.incrementAndGet();
		});
		
		assertThat(cnt.get()).isEqualTo(2);
	}
}
