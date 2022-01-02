package tech.quantit.northstar.domain.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.ConnectionState;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.GatewayDescription;

public class GatewayConnectionTest {
	
	GatewayConnection conn;
	
	@BeforeEach
	public void prepare() {
		CtpSettings settings = new CtpSettings();
		settings.setBrokerId("pingan");
		settings.setPassword("adslfkjals");
		settings.setUserId("kevin");
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("testGateway")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.TRADE)
				.settings(settings)
				.build();
		conn = new GatewayConnection(gd);
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
