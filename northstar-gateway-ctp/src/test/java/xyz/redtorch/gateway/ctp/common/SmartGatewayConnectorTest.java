package xyz.redtorch.gateway.ctp.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SmartGatewayConnectorTest {

	@Test
	void test() {
		SmartGatewayConnector sgc = new SmartGatewayConnector();
		sgc.update();
		String bestEndpoint = sgc.bestEndpoint();
		assertThat(bestEndpoint).isNotBlank();
		System.out.println(bestEndpoint);
	}

}
