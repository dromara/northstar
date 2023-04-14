package org.dromara.northstar.gateway.sim.trade;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;

import org.dromara.northstar.gateway.sim.trade.CloseTradeRequest;
import org.dromara.northstar.gateway.sim.trade.OpenTradeRequest;
import org.dromara.northstar.gateway.sim.trade.SimAccount;
import org.dromara.northstar.gateway.sim.trade.TradePosition;
import org.dromara.northstar.gateway.sim.trade.TradeRequest;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

@SuppressWarnings("unchecked")
class TradeRequestTest {
	
	TestFieldFactory factory = new TestFieldFactory("gateway");

	@Test
	void testOpenOrderOnCancel() {
		SubmitOrderReqField orderReq = factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 2000, 0);
		OpenTradeRequest req = new OpenTradeRequest(mock(SimAccount.class), mock(FastEventEngine.class), mock(Consumer.class));
		req.initOrder(orderReq);
		req.onCancal(factory.makeCancelReq(orderReq));
		verify(req.feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), any(OrderField.class));
		verify(req.doneCallback).accept(any());
	}

	@Test
	void testOpenOrderOnTickTraded() {
		SubmitOrderReqField orderReq = factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 2000, 0);
		OpenTradeRequest req = new OpenTradeRequest(mock(SimAccount.class), mock(FastEventEngine.class), mock(Consumer.class));
		req.initOrder(orderReq);
		req.onTick(factory.makeTickField("rb2210", 1999));
		verify(req.feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), any(OrderField.class));
		verify(req.doneCallback).accept(any());
	}
	
	@Test
	void testOpenOrderOnTickNotTraded() {
		SubmitOrderReqField orderReq = factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 2000, 0);
		OpenTradeRequest req = new OpenTradeRequest(mock(SimAccount.class), mock(FastEventEngine.class), mock(Consumer.class));
		req.initOrder(orderReq);
		req.onTick(factory.makeTickField("rb2210", 2000));
		verify(req.feEngine, times(1)).emitEvent(eq(NorthstarEventType.ORDER), any(OrderField.class));
		verify(req.doneCallback, times(0)).accept(any());
	}
	
	@Test
	void testCloseOrderOnCancel() {
		SubmitOrderReqField orderReq = factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 1, 2000, 0);
		CloseTradeRequest req = new CloseTradeRequest(mock(SimAccount.class), mock(TradePosition.class), mock(FastEventEngine.class), mock(Consumer.class));
		req.initOrder(orderReq);
		req.onCancal(factory.makeCancelReq(orderReq));
		verify(req.feEngine, times(2)).emitEvent(eq(NorthstarEventType.ORDER), any(OrderField.class));
		verify(req.doneCallback).accept(any());
	}
	
	@Test
	void testCloseOrderOnTickNotTraded() {
		SubmitOrderReqField orderReq = factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 1, 2000, 0);
		CloseTradeRequest req = new CloseTradeRequest(mock(SimAccount.class), mock(TradePosition.class), mock(FastEventEngine.class), mock(Consumer.class));
		req.initOrder(orderReq);
		req.onTick(factory.makeTickField("rb2210", 2000));
		verify(req.feEngine, times(1)).emitEvent(eq(NorthstarEventType.ORDER), any(OrderField.class));
		verify(req.doneCallback, times(0)).accept(any());
	}
	
	@Test
	void shouldThrowIfNotMatch() {
		assertThrows(IllegalArgumentException.class, ()->{
			SubmitOrderReqField orderReq = factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Open, 1, 2000, 0);
			TradeRequest req = new CloseTradeRequest(mock(SimAccount.class), mock(TradePosition.class), mock(FastEventEngine.class), mock(Consumer.class));
			req.initOrder(orderReq);
		});
		
		assertThrows(IllegalArgumentException.class, ()->{
			SubmitOrderReqField orderReq = factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 1, 2000, 0);
			TradeRequest req = new OpenTradeRequest(mock(SimAccount.class), mock(FastEventEngine.class), mock(Consumer.class));
			req.initOrder(orderReq);
		});
	}
}
