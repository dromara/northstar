package tech.xuanwu.northstar.trader.domain.simulated;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.gateway.FastEventEngine;
import tech.xuanwu.northstar.gateway.FastEventEngine.EventType;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;

public class SimulatedGatewayTest {
	
	private SimulatedGateway simGateway;
	
	private FastEventEngine feEngine;
	
	private SimulatedMarket simMarket;
	
	@Before
	public void prepare() {
		GatewaySettingField gatewaySetting = GatewaySettingField.newBuilder()
				.setGatewayId("demo")
				.setGatewayName("demo")
				.build();
		feEngine = mock(FastEventEngine.class);
		simMarket = mock(SimulatedMarket.class);
		simGateway = new SimulatedGateway(feEngine, gatewaySetting, simMarket);
	}
	
	@Test
	public void testConnect() {
		
		simGateway.connect();
		verify(feEngine).emitEvent(eq(EventType.LIFECYCLE), anyString(), isA(String.class));
	}
	
	@Test
	public void testDisconnect() {
		
		simGateway.disconnect();
		verify(feEngine).emitEvent(eq(EventType.LIFECYCLE), anyString(), isA(String.class));
	}

	@Test
	public void testSubmitOrder() {
		SubmitOrderReqField submitOrderReq = SubmitOrderReqField.newBuilder().build();
		simGateway.submitOrder(submitOrderReq);
		verify(simMarket).submitOrderReq(same(submitOrderReq));
	}
	
	@Test
	public void testCancelOrder() {
		CancelOrderReqField cancelOrderReq = CancelOrderReqField.newBuilder().build();
		simGateway.cancelOrder(cancelOrderReq);
		verify(simMarket).cancelOrder(same(cancelOrderReq));
	}
}
