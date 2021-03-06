package tech.quantit.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.gateway.api.domain.GlobalMarketRegistry;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

class SimTradeGatewayLocalTest {
	
	private SimTradeGatewayLocal gateway;
	
	private TestFieldFactory factory = new TestFieldFactory("test");
	
	@BeforeEach
	void prepare() {
		FastEventEngine feEngine = mock(FastEventEngine.class);
		GatewaySettingField settings = GatewaySettingField.newBuilder()
				.setGatewayId("gatewayId")
				.build();
		SimAccount simAccount = mock(SimAccount.class);
		gateway = new SimTradeGatewayLocal(feEngine, mock(SimMarket.class), settings, "bindedGatewayId", simAccount, mock(GlobalMarketRegistry.class));
	}

	@Test
	void testConnectAndDisConnect() {
		gateway.connect();
		verify(gateway.feEngine).emitEvent(eq(NorthstarEventType.CONNECTED), anyString());
		
		gateway.disconnect();
		verify(gateway.feEngine).emitEvent(eq(NorthstarEventType.DISCONNECTED), anyString());
	}

	@Test
	void testSubmitOrder() {
		gateway.submitOrder(factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 1, 0, 0));
		verify(gateway.account).onSubmitOrder(any(SubmitOrderReqField.class));
	}

	@Test
	void testCancelOrder() {
		gateway.cancelOrder(factory.makeCancelReq(factory.makeOrderReq("rb2210", DirectionEnum.D_Buy, OffsetFlagEnum.OF_Close, 1, 0, 0)));
		verify(gateway.account).onCancelOrder(any(CancelOrderReqField.class));
	}

	@Test
	void testMoneyIO() {
		gateway.moneyIO(1);
		verify(gateway.account).depositMoney(1);
		gateway.moneyIO(-1);
		verify(gateway.account).withdrawMoney(1);
	}

	@Test
	void testGetAuthErrorFlag() {
		assertThat(gateway.getAuthErrorFlag()).isFalse();
	}

}
