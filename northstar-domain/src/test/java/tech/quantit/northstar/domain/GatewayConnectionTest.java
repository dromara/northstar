package tech.quantit.northstar.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.ConnectionState;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.GatewayDescription;

public class GatewayConnectionTest {
	
	GatewayConnection conn;
	
	@BeforeEach
	public void prepare() {
		CtpSettings settings = new CtpSettings();
		settings.setAppId("app123456");
		settings.setAuthCode("auth321564");
		settings.setBrokerId("pingan");
		settings.setMdHost("127.0.0.1");
		settings.setMdPort("8080");
		settings.setPassword("adslfkjals");
		settings.setTdHost("127.0.0.1");
		settings.setTdPort("8081");
		settings.setUserId("kevin");
		settings.setUserProductInfo("productioninfo");
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("testGateway")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.TRADE)
				.settings(settings)
				.build();
		conn = new TraderGatewayConnection(gd, mock(InternalEventBus.class));
	}
	

	@Test
	public void testOnConnected() {
		conn.onConnected();
		assertThat(conn.gwDescription.getConnectionState()).isEqualTo(ConnectionState.CONNECTED);
	}

	@Test
	public void testOnDisconnected() {
		testOnConnected();
		
		conn.onDisconnected();
		assertThat(conn.gwDescription.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
	}

	@Test
	public void testIsConnected() {
		testOnConnected();
		assertThat(conn.isConnected()).isTrue();
	}

	@Test
	public void testOnError() {
		conn.onError();
		assertThat(conn.errorFlag).isTrue();
	}

	@Test
	public void testHasConnectionError() {
		conn.onError();
		assertThat(conn.hasConnectionError()).isTrue();
	}

}
