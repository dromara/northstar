package org.dromara.northstar.support.utils.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.time.GenericTradeTime;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.strategy.MergedBarListener;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry.ListenerType;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.ContractField;

class BarMergerRegistryTest {
	
	BarMergerRegistry registry = new BarMergerRegistry();

	TestFieldFactory factory = new TestFieldFactory("gateway");
	
	ContractField contract = factory.makeContract("rb2205");
	
	MergedBarListener listener1 = mock(MergedBarListener.class);
	MergedBarListener listener2 = mock(MergedBarListener.class);
	MergedBarListener listener3 = mock(MergedBarListener.class);
	
	@Test
	void test() {
		Contract c = mock(Contract.class);
		when(c.contractField()).thenReturn(contract);
		when(c.tradeTimeDefinition()).thenReturn(new GenericTradeTime());
		registry.addListener(c, 5, PeriodUnit.MINUTE, listener1, ListenerType.INDICATOR);
		registry.addListener(c, 5, PeriodUnit.MINUTE, listener2, ListenerType.INDICATOR);
		
		assertThat(registry.mergerListenerMap.keySet()).hasSize(1);
	}

}
