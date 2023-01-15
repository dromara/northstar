package tech.quantit.northstar.strategy.api.utils.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.IModuleStrategyContext;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;

class TradeIntentTest {
	
	IModuleStrategyContext ctx = mock(IModuleStrategyContext.class);
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	ContractField contract = factory.makeContract("rb2305");
	
	String ORDER_ID = "123456";
	
	@BeforeEach
	void prepare() {
		when(ctx.submitOrderReq(any(ContractField.class), any(SignalOperation.class), any(PriceType.class), anyInt(), any(Double.class)))
			.thenReturn(Optional.of(ORDER_ID));
	}

	@Test
	void testSimpleOpen() {
		TradeIntent intent = TradeIntent.builder()
				.contract(contract).operation(SignalOperation.BUY_OPEN).priceType(PriceType.OPP_PRICE).volume(1).build();
		intent.setContext(ctx);
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5000));
		intent.onTick(factory.makeTickField("rb2305", 5000));
		intent.onTrade(factory.makeTradeField("rb2305", 5000, 1, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open));
		assertThat(intent.hasTerminated()).isTrue();
		
		verify(ctx, times(1)).submitOrderReq(any(ContractField.class), any(SignalOperation.class), any(PriceType.class), anyInt(), any(Double.class));
		verify(ctx, times(0)).cancelOrder(anyString());
	}

	@Test
	void testSimpleClose() {
		TradeIntent intent = TradeIntent.builder()
				.contract(contract).operation(SignalOperation.SELL_CLOSE).priceType(PriceType.OPP_PRICE).volume(1).build();
		intent.setContext(ctx);		
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5000));
		intent.onTrade(factory.makeTradeField("rb2305", 5000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close));
		assertThat(intent.hasTerminated()).isTrue();
		
		verify(ctx, times(1)).submitOrderReq(any(ContractField.class), any(SignalOperation.class), any(PriceType.class), anyInt(), any(Double.class));
		verify(ctx, times(0)).cancelOrder(anyString());
	}
	
	@Test
	void testTimeoutRetryOpen() {
		when(ctx.isOrderWaitTimeout(anyString(), anyLong())).thenReturn(true, false);
		TradeIntent intent = TradeIntent.builder()
				.contract(contract).operation(SignalOperation.SELL_CLOSE).priceType(PriceType.OPP_PRICE).volume(1).build();
		intent.setContext(ctx);
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5000));
		intent.onTick(factory.makeTickField("rb2305", 5000));
		intent.onOrder(OrderField.newBuilder().setOriginOrderId(ORDER_ID).setOrderStatus(OrderStatusEnum.OS_Canceled).build());
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5000));
		intent.onTrade(factory.makeTradeField("rb2305", 5000, 1, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Close));
		assertThat(intent.hasTerminated()).isTrue();
		
		verify(ctx, times(2)).submitOrderReq(any(ContractField.class), any(SignalOperation.class), any(PriceType.class), anyInt(), any(Double.class));
		verify(ctx, times(1)).cancelOrder(anyString());
	}
	
	@Test
	void testOpenAbort() {
		TradeIntent intent = TradeIntent.builder()
				.contract(contract).operation(SignalOperation.SELL_CLOSE).priceType(PriceType.OPP_PRICE).volume(1)
				.abortCondition(tick -> tick.getLastPrice() - 5000 > 10)
				.build();
		intent.setContext(ctx);
		intent.onTick(factory.makeTickField("rb2305", 5000));
		assertThat(intent.hasTerminated()).isFalse();
		intent.onTick(factory.makeTickField("rb2305", 5020));
		assertThat(intent.hasTerminated()).isTrue();
	}
}
