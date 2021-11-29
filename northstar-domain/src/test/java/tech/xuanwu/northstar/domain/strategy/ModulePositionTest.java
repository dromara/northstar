package tech.xuanwu.northstar.domain.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.junit.Test;

import tech.xuanwu.northstar.strategy.api.event.ModuleEvent;
import tech.xuanwu.northstar.strategy.api.event.ModuleEventBus;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModulePositionTest {
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	String SYMBOL = "rb2210";
	TradeField buyTrade = factory.makeTradeField(SYMBOL, 2000, 10, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TradeField sellTrade = factory.makeTradeField(SYMBOL, 2000, 10, DirectionEnum.D_Sell, OffsetFlagEnum.OF_Open);
	TradeField closeTrade = factory.makeTradeField(SYMBOL, 2000, 10, DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open);
	TickField tick = factory.makeTickField(SYMBOL, 2100);
	TickField tick2 = factory.makeTickField("rb2110", 2100);
	@Test
	public void shouldUpdateProfit() {
		ModulePosition p1 = new ModulePosition(buyTrade, 0);
		p1.onTick(tick);
		assertThat(p1.getProfit()).isEqualTo(10000);
		
		ModulePosition p2 = new ModulePosition(sellTrade, 0);
		p2.onTick(tick);
		assertThat(p2.getProfit()).isEqualTo(-10000);
	}
	
	@Test
	public void shouldNotUpdateProfit() {
		ModulePosition p1 = new ModulePosition(buyTrade, 0);
		p1.onTick(tick2);
		assertThat(p1.getProfit()).isEqualTo(0);
	}

	@Test
	public void shouldTriggerStopLoss() {
		Consumer<ModulePosition> callback = mock(Consumer.class);
		ModulePosition p1 = new ModulePosition(sellTrade, 2100, callback);
		p1.meb = mock(ModuleEventBus.class);
		p1.onTick(tick);
		verify(p1.meb).post(any(ModuleEvent.class));
		verify(callback, times(0)).accept(any(ModulePosition.class));
	}
	
	@Test
	public void shouldNotTriggerStopLoss() {
		ModulePosition p1 = new ModulePosition(sellTrade, 2200);
		p1.meb = mock(ModuleEventBus.class);
		p1.onTick(tick);
		verify(p1.meb, times(0)).post(any(ModuleEvent.class));
	}
	
	@Test
	public void testClosePositionAndOrderTraded() {
		Consumer<ModulePosition> callback = mock(Consumer.class);
		ModulePosition p1 = new ModulePosition(sellTrade, 2100, callback);
		p1.lastTick = tick;
		SubmitOrderReqField submitOrderReq = p1.closePosition(10, 1900);
		p1.onTrade(TradeField.newBuilder()
				.setOriginOrderId(submitOrderReq.getOriginOrderId())
				.setVolume(submitOrderReq.getVolume())
				.build());
		assertThat(p1.getVolume()).isZero();
		assertThat(p1.availableVol).isZero();
		verify(callback).accept(any(ModulePosition.class));
	}
	
	@Test
	public void testClosePositionAndOrderPartiallyTraded() {
		ModulePosition p1 = new ModulePosition(sellTrade, 2100);
		p1.lastTick = tick;
		SubmitOrderReqField submitOrderReq = p1.closePosition(10, 1900);
		p1.onTrade(TradeField.newBuilder()
				.setOriginOrderId(submitOrderReq.getOriginOrderId())
				.setVolume(4)
				.build());
		assertThat(p1.getVolume()).isEqualTo(6);
		assertThat(p1.availableVol).isZero();
		
		p1.onOrder(OrderField.newBuilder()
				.setOriginOrderId(submitOrderReq.getOriginOrderId())
				.setTradedVolume(4)
				.setTotalVolume(10)
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.build());
		assertThat(p1.getVolume()).isEqualTo(6);
		assertThat(p1.availableVol).isEqualTo(6);
	}
	
	@Test
	public void testClosePositionAndOrderCancelled() {
		ModulePosition p1 = new ModulePosition(sellTrade, 2100);
		p1.lastTick = tick;
		SubmitOrderReqField submitOrderReq = p1.closePosition(8, 1900);
		p1.onOrder(OrderField.newBuilder()
				.setOriginOrderId(submitOrderReq.getOriginOrderId())
				.setTradedVolume(0)
				.setTotalVolume(8)
				.setOrderStatus(OrderStatusEnum.OS_Canceled)
				.build());
		assertThat(p1.getVolume()).isEqualTo(10);
		assertThat(p1.availableVol).isEqualTo(10);
	}
	
	@Test
	public void testOpenPrice() {
		ModulePosition p1 = new ModulePosition(sellTrade, 2100);
		assertThat(p1.openPrice()).isEqualTo(2000);
	}
	
	@Test
	public void testSetEventBus() {
		ModulePosition p1 = new ModulePosition(sellTrade, 2100);
		ModuleEventBus meb = mock(ModuleEventBus.class);
		p1.setEventBus(meb);
		assertThat(p1.meb).isEqualTo(meb);
	}
}
