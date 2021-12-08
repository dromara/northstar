package tech.quantit.northstar.domain.strategy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;
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
	
	
	@SuppressWarnings("unchecked")
	Consumer<TradeField> openCallback = mock(Consumer.class);
	
	@SuppressWarnings("unchecked")
	Consumer<ModuleDealRecord> closeCallback = mock(Consumer.class);

	@SuppressWarnings("unchecked")
	Consumer<Boolean> fallback = mock(Consumer.class);
	
	@Test
	public void testAllTradedForOpenOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 4, 1000, 0);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, orderReq, openCallback, closeCallback, fallback);
		
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
		
		verify(openCallback).accept(any(TradeField.class));
	}
	
	
	@Test
	public void testPartiallyTradedForOpenOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 4, 1000, 0);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, orderReq, openCallback, closeCallback, fallback);
		
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
		
		verify(openCallback).accept(any(TradeField.class));
	}
	
	
	@Test
	public void testPartiallyTradedForOpenOrder2() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open, 4, 1000, 0);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, orderReq, openCallback, closeCallback, fallback);
		
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
		
		verify(openCallback).accept(any(TradeField.class));
	}
	
	
	@Test
	public void testNonTradedForOpenOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 4, 1000, 0);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, orderReq, openCallback, closeCallback, fallback);
		
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(0)
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.build());
		
		verify(fallback).accept(any());
	}
	
	
	@Test
	public void testAllTradedForCloseOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 4, 1000, 0);
		ModulePosition mp = mock(ModulePosition.class);
		when(mp.getOpenPrice()).thenReturn(1010D);
		when(mp.getDirection()).thenReturn(PositionDirectionEnum.PD_Long);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, mp, orderReq, mock(Consumer.class), closeCallback, fallback);
		
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
		
		verify(closeCallback).accept(any(ModuleDealRecord.class));
	}

	@Test
	public void testPartiallyTradedForCloseOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 4, 1000, 0);
		ModulePosition mp = mock(ModulePosition.class);
		when(mp.getOpenPrice()).thenReturn(1010D);
		when(mp.getDirection()).thenReturn(PositionDirectionEnum.PD_Long);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, mp, orderReq, mock(Consumer.class), closeCallback, fallback);
		
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
		
		verify(closeCallback).accept(any(ModuleDealRecord.class));
	}
	
	@Test
	public void testNonTradedForCloseOrder() {
		SubmitOrderReqField orderReq = factory.makeOrderReq(SYMBOL, DirectionEnum.D_Sell, OffsetFlagEnum.OF_CloseToday, 4, 1000, 0);
		ModulePosition mp = mock(ModulePosition.class);
		when(mp.getOpenPrice()).thenReturn(1010D);
		when(mp.getDirection()).thenReturn(PositionDirectionEnum.PD_Long);
		ModuleTradeIntent mti = new ModuleTradeIntent(NAME, mp, orderReq, mock(Consumer.class), closeCallback, fallback);
		
		mti.onOrder(OrderField.newBuilder()
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setTotalVolume(orderReq.getVolume())
				.setTradedVolume(0)
				.setOffsetFlag(orderReq.getOffsetFlag())
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.build());
		
		verify(fallback).accept(any());
	}
	
}
