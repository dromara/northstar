package org.dromara.northstar.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.IModuleContext;
import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;

class DisposablePriceListenerTest {

	Contract contract = Contract.builder().unifiedSymbol("rb2210").multiplier(10).priceTick(1).build();

	IModuleContext ctx = mock(IModuleContext.class);

	@Test
	void testBuyEarn() {
		DisposablePriceListener listener = DisposablePriceListener.create(ctx, contract, DirectionEnum.D_Buy, 5000D, 100, 1);
		Tick t1 = Tick.builder().contract(contract).lastPrice(5099).build();
		assertThat(listener.shouldBeTriggered(t1)).isFalse();

		Tick t2 = Tick.builder().contract(contract).lastPrice(5100).build();
		assertThat(listener.shouldBeTriggered(t2)).isTrue();

		Tick t3 = Tick.builder().contract(contract).lastPrice(4800).build();
		assertThat(listener.shouldBeTriggered(t3)).isFalse();
	}

	@Test
	void testBuyLoss(){
		DisposablePriceListener listener = DisposablePriceListener.create(ctx, contract, DirectionEnum.D_Buy, 5000D, -100, 1);
		Tick t1 = Tick.builder().contract(contract).lastPrice(4901).build();
		assertThat(listener.shouldBeTriggered(t1)).isFalse();

		Tick t2 = Tick.builder().contract(contract).lastPrice(4900).build();
		assertThat(listener.shouldBeTriggered(t2)).isTrue();

		Tick t3 = Tick.builder().contract(contract).lastPrice(5100).build();
		assertThat(listener.shouldBeTriggered(t3)).isFalse();
	}


	@Test
	void testSellEarn() {
		DisposablePriceListener listener = DisposablePriceListener.create(ctx, contract, DirectionEnum.D_Sell, 5000D, 100, 1);
		Tick t1 = Tick.builder().contract(contract).lastPrice(4901).build();
		assertThat(listener.shouldBeTriggered(t1)).isFalse();

		Tick t2 = Tick.builder().contract(contract).lastPrice(4900).build();
		assertThat(listener.shouldBeTriggered(t2)).isTrue();

		Tick t3 = Tick.builder().contract(contract).lastPrice(5100).build();
		assertThat(listener.shouldBeTriggered(t3)).isFalse();
	}

	@Test
	void testSellLoss() {
		DisposablePriceListener listener = DisposablePriceListener.create(ctx, contract, DirectionEnum.D_Sell, 5000D, -100, 1);
		Tick t1 = Tick.builder().contract(contract).lastPrice(5099).build();
		assertThat(listener.shouldBeTriggered(t1)).isFalse();

		Tick t2 = Tick.builder().contract(contract).lastPrice(5100).build();
		assertThat(listener.shouldBeTriggered(t2)).isTrue();

		Tick t3 = Tick.builder().contract(contract).lastPrice(4800).build();
		assertThat(listener.shouldBeTriggered(t3)).isFalse();
	}
}
