package tech.xuanwu.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import tech.xuanwu.northstar.strategy.api.model.ModuleDealRecord;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModuleTradeIntentTest {
	
	TestFieldFactory factory = new TestFieldFactory("test");
	String NAME = "testModule";
	String SYMBOL = "rb2210";
	
	Consumer<Optional<ModulePosition>> openCallback = mock(Consumer.class);
	Consumer<Optional<ModuleDealRecord>> closeCallback = mock(Consumer.class);

	@SuppressWarnings("unchecked")
	@Test
	public void testAllTradedForOpenOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 4, 1000, 0);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, orderReq, openCallback);
		
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(orderReq.getVolume())
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		mti.onTrade(TradeField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setVolume(orderReq.getVolume())
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		
		ArgumentCaptor<Optional<ModulePosition>> arg = ArgumentCaptor.forClass(Optional.class);
		verify(openCallback).accept(arg.capture());
		assertThat(arg.getValue()).isNotEmpty();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testPartiallyTradedForOpenOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 4, 1000, 0);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, orderReq, openCallback);
		
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(2)
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		mti.onTrade(TradeField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setVolume(orderReq.getVolume())
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		
		ArgumentCaptor<Optional<ModulePosition>> arg = ArgumentCaptor.forClass(Optional.class);
		verify(openCallback).accept(arg.capture());
		assertThat(arg.getValue()).isNotEmpty();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testPartiallyTradedForOpenOrder2() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 4, 1000, 0);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, orderReq, openCallback);
		
		mti.onTrade(TradeField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setVolume(2)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(2)
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		
		ArgumentCaptor<Optional<ModulePosition>> arg = ArgumentCaptor.forClass(Optional.class);
		verify(openCallback).accept(arg.capture());
		assertThat(arg.getValue()).isNotEmpty();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testNonTradedForOpenOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 4, 1000, 0);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, orderReq, openCallback);
		
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(0)
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		
		ArgumentCaptor<Optional<ModulePosition>> arg = ArgumentCaptor.forClass(Optional.class);
		verify(openCallback).accept(arg.capture());
		assertThat(arg.getValue()).isEmpty();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAllTradedForCloseOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 4, 1000, 0);
		ModulePosition mp = mock(ModulePosition.class);
		when(mp.openPrice()).thenReturn(1010D);
		when(mp.getDirection()).thenReturn(PositionDirectionEnum.PD_Long);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, mp, orderReq, closeCallback);
		
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(orderReq.getVolume())
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		mti.onTrade(TradeField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setVolume(orderReq.getVolume())
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		
		ArgumentCaptor<Optional<ModuleDealRecord>> arg = ArgumentCaptor.forClass(Optional.class);
		verify(closeCallback).accept(arg.capture());
		assertThat(arg.getValue()).isNotEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPartiallyTradedForCloseOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 4, 1000, 0);
		ModulePosition mp = mock(ModulePosition.class);
		when(mp.openPrice()).thenReturn(1010D);
		when(mp.getDirection()).thenReturn(PositionDirectionEnum.PD_Long);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, mp, orderReq, closeCallback);
		
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(2)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.setOrderStatus(OrderStatusEnum.OS_PartTradedQueueing)
				.build());
		mti.onTrade(TradeField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setVolume(2)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(2)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.build());
		
		ArgumentCaptor<Optional<ModuleDealRecord>> arg = ArgumentCaptor.forClass(Optional.class);
		verify(closeCallback).accept(arg.capture());
		assertThat(arg.getValue()).isNotEmpty();
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testNonTradedForCloseOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 4, 1000, 0);
		ModulePosition mp = mock(ModulePosition.class);
		when(mp.openPrice()).thenReturn(1010D);
		when(mp.getDirection()).thenReturn(PositionDirectionEnum.PD_Long);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, mp, orderReq, closeCallback);
		
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(0)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.build());
		
		ArgumentCaptor<Optional<ModuleDealRecord>> arg = ArgumentCaptor.forClass(Optional.class);
		verify(closeCallback).accept(arg.capture());
		assertThat(arg.getValue()).isEmpty();
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowIfUseWrongConstuction() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 4, 1000, 0);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, orderReq, openCallback);
	}
	
	@Test(expected = IllegalStateException.class)
	public void shouldThrowIfUseWrongConstuction2() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 4, 1000, 0);
		ModulePosition mp = mock(ModulePosition.class);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, mp, orderReq, closeCallback);
	}
}
