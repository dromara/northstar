package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.MessageSourceHolder;
import org.dromara.northstar.gateway.IMarketCenter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

class SimTradeGatewayTest {

	private FastEventEngine feEngine;
	private SimAccountRemote simAccount;
	private IMarketCenter mktCenter;
	private SimTradeGateway gateway;
	private GatewayDescription gd;
	
	@BeforeAll
	static void init() {
		MessageSourceHolder.setMessageSource(mock(MessageSource.class));
	}

	@BeforeEach
	void setUp() {
		feEngine = mock(FastEventEngine.class);
		simAccount = mock(SimAccountRemote.class);
		mktCenter = mock(IMarketCenter.class);
		
		gd = GatewayDescription.builder()
				.gatewayId("TEST_GATEWAY")
				.build();
		
		gateway = new SimTradeGateway(feEngine, gd, simAccount, mktCenter);
	}

	@Test
	void testConnect() {
		gateway.connect();
		
		// Verify connect behavior by checking it doesn't throw when submitting order
		assertThatNoException().isThrownBy(() -> {
			SubmitOrderReq req = SubmitOrderReq.builder()
					.gatewayId("TEST_GATEWAY")
					.build();
			gateway.submitOrder(req);
		});
		verify(feEngine).emitEvent(NorthstarEventType.LOGGED_IN, "TEST_GATEWAY");
	}

	@Test
	void testDisconnect() {
		gateway.connect();
		gateway.disconnect();
		
		// Verify disconnect behavior by checking it throws when submitting order
		assertThatThrownBy(() -> {
			SubmitOrderReq req = SubmitOrderReq.builder()
					.gatewayId("TEST_GATEWAY")
					.build();
			gateway.submitOrder(req);
		}).isInstanceOf(OperationException.class);
		verify(feEngine).emitEvent(NorthstarEventType.LOGGED_OUT, "TEST_GATEWAY");
	}

	@Test
	void testSubmitOrderWhenConnected() throws Exception {
		gateway.connect();
		
		SubmitOrderReq req = SubmitOrderReq.builder()
				.gatewayId("TEST_GATEWAY")
				.build();
		
		gateway.submitOrder(req);
		
		verify(simAccount).submitOrder(req);
	}

	@Test
	void testSubmitOrderWhenDisconnected() {
		SubmitOrderReq req = SubmitOrderReq.builder()
				.gatewayId("TEST_GATEWAY")
				.build();
		
		assertThatThrownBy(() -> gateway.submitOrder(req))
			.isInstanceOf(OperationException.class);
		
		verify(simAccount, never()).submitOrder(any());
	}

	@Test
	void testCancelOrderWhenConnected() {
		gateway.connect();
		
		CancelOrderReq req = CancelOrderReq.builder()
				.originOrderId("ORDER_1")
				.build();
		
		gateway.cancelOrder(req);
		verify(simAccount).cancelOrder(req);
	}

	@Test
	void testCancelOrderWhenDisconnected() {
		CancelOrderReq req = CancelOrderReq.builder()
				.originOrderId("ORDER_1")
				.build();
		
		assertThatThrownBy(() -> gateway.cancelOrder(req))
			.isInstanceOf(OperationException.class);
		
		verify(simAccount, never()).cancelOrder(any());
	}

	@Test
	void testOnTickForTradableContract() {
		Contract contract = Contract.builder()
				.tradable(true)
				.build();
		Tick tick = Tick.builder()
				.contract(contract)
				.build();
		
		gateway.onTick(tick);
		
		verify(mktCenter).onTick(tick);
		verify(simAccount).onTick(tick);
	}

	@Test
	void testOnTickForNonTradableContract() {
		Contract contract = Contract.builder()
				.tradable(false)
				.build();
		Tick tick = Tick.builder()
				.contract(contract)
				.build();
		
		gateway.onTick(tick);
		
		verify(mktCenter, never()).onTick(tick);
		verify(simAccount).onTick(tick);
	}

	@Test
	void testOnAccount() {
		org.dromara.northstar.common.model.core.Account account = org.dromara.northstar.common.model.core.Account.builder()
				.gatewayId("TEST_GATEWAY")
				.build();
		
		gateway.onAccount(account);
		
		verify(feEngine).emitEvent(NorthstarEventType.ACCOUNT, account);
	}

	@Test
	void testOnPosition() {
		org.dromara.northstar.common.model.core.Position position = org.dromara.northstar.common.model.core.Position.builder()
				.gatewayId("TEST_GATEWAY")
				.build();
		
		gateway.onPosition(position);
		
		verify(feEngine).emitEvent(NorthstarEventType.POSITION, position);
	}

	@Test
	void testOnOrder() {
		org.dromara.northstar.common.model.core.Order order = org.dromara.northstar.common.model.core.Order.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(Contract.builder().build())
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.build();
		
		gateway.onOrder(order);
		
		verify(feEngine).emitEvent(NorthstarEventType.ORDER, order);
	}

	@Test
	void testOnTrade() {
		org.dromara.northstar.common.model.core.Trade trade = org.dromara.northstar.common.model.core.Trade.builder()
				.gatewayId("TEST_GATEWAY")
				.contract(Contract.builder().build())
				.direction(DirectionEnum.D_Buy)
				.offsetFlag(OffsetFlagEnum.OF_Open)
				.build();
		
		gateway.onTrade(trade);
		
		verify(feEngine).emitEvent(NorthstarEventType.TRADE, trade);
	}

	@Test
	void testConnectionState() {
		// Initially disconnected
		assertThatThrownBy(() -> {
			SubmitOrderReq req = SubmitOrderReq.builder()
					.gatewayId("TEST_GATEWAY")
					.build();
			gateway.submitOrder(req);
		}).isInstanceOf(OperationException.class);
		
		gateway.connect();
		// After connect, should not throw
		assertThatNoException().isThrownBy(() -> {
			SubmitOrderReq req = SubmitOrderReq.builder()
					.gatewayId("TEST_GATEWAY")
					.build();
			gateway.submitOrder(req);
		});
		
		gateway.disconnect();
		// After disconnect, should throw again
		assertThatThrownBy(() -> {
			SubmitOrderReq req = SubmitOrderReq.builder()
					.gatewayId("TEST_GATEWAY")
					.build();
			gateway.submitOrder(req);
		}).isInstanceOf(OperationException.class);
	}
}
