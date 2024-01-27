package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.strategy.IModuleStrategyContext;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class TradeHelperTest {
	
	TradeHelper hlp;
	
	Contract contract = Contract.builder().unifiedSymbol("test").build();
	
	IModuleStrategyContext ctx = mock(IModuleStrategyContext.class);
	
	@BeforeEach
	void prepare() {
		when(ctx.getLogger(any())).thenReturn(mock(Logger.class));
		hlp = new TradeHelper(ctx, contract);
	}
	
	@SuppressWarnings("static-access")
	@Test
	void testSpreadRateInPercentage() {
		Tick t1 = Tick.builder().lastPrice(1000).askPrice(List.of(1001D)).bidPrice(List.of(999D)).build();
		Tick t2 = Tick.builder().lastPrice(1100).askPrice(List.of(1101D)).bidPrice(List.of(1099D)).build();
		assertThat(hlp.spreadRateInPercentage(t1, t2, true)).isCloseTo(100.0 * (1001 - 1099) / 1099, offset(1e-6));
		assertThat(hlp.spreadRateInPercentage(t1, t2, false)).isCloseTo(100.0 * (999 - 1101) / 1101, offset(1e-6));
	}

	@Test
	void testDoBuyOpenDoubleIntLongPredicateOfDouble() {
		hlp.doBuyOpen(1000, 5, 1000, p -> true);
		verify(ctx).submitOrderReq(any(TradeIntent.class));
	}

	@Test
	void testDoBuyOpenInt() {
		hlp.doBuyOpen(1);
		verify(ctx).submitOrderReq(any(TradeIntent.class));
	}

	@Test
	void testDoSellOpenDoubleIntLongPredicateOfDouble() {
		hlp.doSellOpen(1000, 5, 10000, p -> true);
		verify(ctx).submitOrderReq(any(TradeIntent.class));
	}

	@Test
	void testDoSellOpenInt() {
		hlp.doSellOpen(1);
		verify(ctx).submitOrderReq(any(TradeIntent.class));
	}

	@Test
	void testDoBuyCloseDoubleIntLongPredicateOfDouble() {
		hlp.doBuyClose(1000, 5, 10000, p -> true);
		verify(ctx).submitOrderReq(any(TradeIntent.class));
	}

	@Test
	void testDoBuyCloseInt() {
		hlp.doBuyClose(1);
		verify(ctx).submitOrderReq(any(TradeIntent.class));
	}

	@Test
	void testDoSellCloseDoubleIntLongPredicateOfDouble() {
		hlp.doSellClose(1000, 5, 10000, p -> true);
		verify(ctx).submitOrderReq(any(TradeIntent.class));
	}

	@Test
	void testDoSellCloseInt() {
		hlp.doSellClose(1);
		verify(ctx).submitOrderReq(any(TradeIntent.class));
	}

}
