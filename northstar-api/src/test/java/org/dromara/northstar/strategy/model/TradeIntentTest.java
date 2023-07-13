package org.dromara.northstar.strategy.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.strategy.IModuleAccount;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.constant.PriceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TradeField;

class TradeIntentTest {
	
	IModuleContext ctx = mock(IModuleContext.class);
	
	IModuleAccount macc = mock(IModuleAccount.class);
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	ContractField contract = factory.makeContract("rb2305");
	
	String ORDER_ID = "123456";
	
	@BeforeEach
	void prepare() {
		when(ctx.submitOrderReq(any(ContractField.class), any(SignalOperation.class), any(PriceType.class), anyInt(), any(Double.class)))
			.thenReturn(Optional.of(ORDER_ID));
		when(ctx.getState()).thenReturn(ModuleState.EMPTY);
		when(ctx.getModuleAccount()).thenReturn(macc);
		when(ctx.getLogger()).thenReturn(mock(Logger.class));
		when(macc.getNonclosedPosition(anyString(), any(DirectionEnum.class), eq(true))).thenReturn(1);
	}

	@Test
	void testSimpleOpen() {
		TradeIntent intent = TradeIntent.builder()
				.contract(contract).operation(SignalOperation.BUY_OPEN).priceType(PriceType.OPP_PRICE).volume(1).timeout(3000).build();
		intent.setContext(ctx);
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5000));
		intent.onTick(factory.makeTickField("rb2305", 5000));
		TradeField trade = factory.makeTradeField("rb2305", 5000, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
		intent.onTrade(trade.toBuilder().setOriginOrderId(ORDER_ID).build());
		assertThat(intent.hasTerminated()).isTrue();
		
		verify(ctx, times(1)).submitOrderReq(any(ContractField.class), any(SignalOperation.class), any(PriceType.class), anyInt(), any(Double.class));
		verify(ctx, times(0)).cancelOrder(anyString());
	}

	@Test
	void testSimpleClose() {
		TradeIntent intent = TradeIntent.builder()
				.contract(contract).operation(SignalOperation.SELL_CLOSE).priceType(PriceType.OPP_PRICE).volume(1).timeout(3000).build();
		intent.setContext(ctx);		
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5000));
		TradeField trade = factory.makeTradeField("rb2305", 5000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
		intent.onTrade(trade.toBuilder().setOriginOrderId(ORDER_ID).build());
		assertThat(intent.hasTerminated()).isTrue();
		
		verify(ctx, times(1)).submitOrderReq(any(ContractField.class), any(SignalOperation.class), any(PriceType.class), anyInt(), any(Double.class));
		verify(ctx, times(0)).cancelOrder(anyString());
	}
	
	@Test
	void testTimeoutRetryOpen() throws InterruptedException {
		when(ctx.isOrderWaitTimeout(anyString(), anyLong())).thenReturn(true, false);
		TradeIntent intent = TradeIntent.builder()
				.contract(contract).operation(SignalOperation.SELL_CLOSE).priceType(PriceType.OPP_PRICE).volume(1).timeout(3000).build();
		intent.setContext(ctx);
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5000));
		intent.onTick(factory.makeTickField("rb2305", 5000));
		intent.onOrder(OrderField.newBuilder().setOriginOrderId(ORDER_ID).setOrderStatus(OrderStatusEnum.OS_Canceled).build());
		Thread.sleep(3100);
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5000));
		TradeField trade = factory.makeTradeField("rb2305", 5000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close);
		intent.onTrade(trade.toBuilder().setOriginOrderId(ORDER_ID).build());
		assertThat(intent.hasTerminated()).isTrue();
		
		verify(ctx, times(2)).submitOrderReq(any(ContractField.class), any(SignalOperation.class), any(PriceType.class), anyInt(), any(Double.class));
		verify(ctx, times(1)).cancelOrder(anyString());
	}
	
	@Test
	void testOpenAbort() {
		TradeIntent intent = TradeIntent.builder()
				.contract(contract).operation(SignalOperation.SELL_CLOSE).priceType(PriceType.OPP_PRICE).volume(1).timeout(3000)
				.priceDiffConditionToAbort(diff -> diff > 10)
				.build();
		intent.setContext(ctx);
		intent.onTick(factory.makeTickField("rb2305", 5000));
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5020));
		assertThat(intent.hasTerminated()).isTrue();
	}
}
