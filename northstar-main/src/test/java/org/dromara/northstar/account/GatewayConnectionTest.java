package org.dromara.northstar.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.account.GatewayConnection;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.model.GatewayDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSONObject;

public class GatewayConnectionTest {
	
	GatewayConnection conn;
	
	@BeforeEach
	public void prepare() {
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("testGateway")
				.channelType(ChannelType.CTP)
				.gatewayUsage(GatewayUsage.TRADE)
				.settings(new JSONObject())
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
