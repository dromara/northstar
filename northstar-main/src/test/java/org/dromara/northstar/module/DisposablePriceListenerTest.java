package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.dromara.northstar.module.DisposablePriceListener;
import org.dromara.northstar.strategy.IModuleContext;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

class DisposablePriceListenerTest {
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	ContractField contract = factory.makeContract("rb2210"); 

	IModuleContext ctx = mock(IModuleContext.class);
	
	@Test
	void testBuyEarn() {
		DisposablePriceListener listener = DisposablePriceListener.create(ctx, contract, DirectionEnum.D_Buy, 5000D, 100, 1);
		TickField t1 = factory.makeTickField("rb2210", 5099);
		assertThat(listener.shouldBeTriggered(t1)).isFalse();

		TickField t2 = factory.makeTickField("rb2210", 5100);
		assertThat(listener.shouldBeTriggered(t2)).isTrue();
		
		TickField t3 = factory.makeTickField("rb2210", 4800);
		assertThat(listener.shouldBeTriggered(t3)).isFalse();
	}
	
	@Test 
	void testBuyLoss(){
		DisposablePriceListener listener = DisposablePriceListener.create(ctx, contract, DirectionEnum.D_Buy, 5000D, -100, 1);
		TickField t1 = factory.makeTickField("rb2210", 4901);
		assertThat(listener.shouldBeTriggered(t1)).isFalse();

		TickField t2 = factory.makeTickField("rb2210", 4900);
		assertThat(listener.shouldBeTriggered(t2)).isTrue();
		
		TickField t3 = factory.makeTickField("rb2210", 5100);
		assertThat(listener.shouldBeTriggered(t3)).isFalse();
	}

	
	@Test
	void testSellEarn() {
		DisposablePriceListener listener = DisposablePriceListener.create(ctx, contract, DirectionEnum.D_Sell, 5000D, 100, 1);
		TickField t1 = factory.makeTickField("rb2210", 4901);
		assertThat(listener.shouldBeTriggered(t1)).isFalse();

		TickField t2 = factory.makeTickField("rb2210", 4900);
		assertThat(listener.shouldBeTriggered(t2)).isTrue();
		
		TickField t3 = factory.makeTickField("rb2210", 5100);
		assertThat(listener.shouldBeTriggered(t3)).isFalse();
	}
	
	@Test
	void testSellLoss() {
		DisposablePriceListener listener = DisposablePriceListener.create(ctx, contract, DirectionEnum.D_Sell, 5000D, -100, 1);
		TickField t1 = factory.makeTickField("rb2210", 5099);
		assertThat(listener.shouldBeTriggered(t1)).isFalse();

		TickField t2 = factory.makeTickField("rb2210", 5100);
		assertThat(listener.shouldBeTriggered(t2)).isTrue();
		
		TickField t3 = factory.makeTickField("rb2210", 4800);
		assertThat(listener.shouldBeTriggered(t3)).isFalse();
	}
}
