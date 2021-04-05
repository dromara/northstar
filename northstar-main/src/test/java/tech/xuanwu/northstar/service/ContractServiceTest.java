package tech.xuanwu.northstar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.common.exception.NoSuchElementException;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.domain.GatewayConnection;
import xyz.redtorch.pb.CoreField.ContractField;

public class ContractServiceTest {
	
	ContractService service = new ContractService();
	
	@Before
	public void prepare() {
		service.gatewayContractMap = new ConcurrentHashMap<>();
	}
	
	@Test
	public void testOnEvent() {
		GatewayConnection conn = mock(GatewayConnection.class);
		when(conn.getGwDescription()).thenReturn(mock(GatewayDescription.class));
		when(conn.getGwDescription().getGatewayId()).thenReturn("testGateway");
		service.onEvent(new NorthstarEvent(NorthstarEventType.CONNECTING, conn));
		
		ContractField contract = ContractField.newBuilder()
				.setGatewayId("testGateway")
				.setSymbol("rb2102")
				.build();
		service.onEvent(new NorthstarEvent(NorthstarEventType.CONTRACT, contract));
		
		assertThat(service.gatewayContractMap.size()).isEqualTo(1);
		assertThat(service.gatewayContractMap.containsKey("testGateway")).isTrue();
	}
	
	@Test(expected = NoSuchElementException.class)
	public void testOnEventWithException() {
		ContractField contract = ContractField.newBuilder()
				.setGatewayId("testGateway")
				.setSymbol("rb2102")
				.build();
		service.onEvent(new NorthstarEvent(NorthstarEventType.CONTRACT, contract));
		
	}

}
