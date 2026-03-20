package org.dromara.northstar.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.GatewayVO;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.data.jdbc.GatewayDescriptionRepository;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.strategy.IMessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
class GatewayServiceTest {
	
	@MockBean
	IMessageSender sender;
	
	@Autowired
	GatewayService service;
	
	@Autowired
	GatewayManager gatewayMgr;
	
	@Autowired
	GatewayDescriptionRepository repo;
	
	GatewayDescription gd1 = GatewayDescription.builder()
			.gatewayId("test")
			.gatewayUsage(GatewayUsage.MARKET_DATA)
			.channelType(ChannelType.SIM)
			.settings(new Object())
			.build();
	
	GatewayDescription gd2 = GatewayDescription.builder()
			.gatewayId("test")
			.gatewayUsage(GatewayUsage.TRADE)
			.channelType(ChannelType.SIM)
			.settings(new Object())
			.bindedMktGatewayId("test2")
			.build();
	
	GatewayDescription gd4 = GatewayDescription.builder()
			.gatewayId("test2")
			.gatewayUsage(GatewayUsage.TRADE)
			.channelType(ChannelType.SIM)
			.settings(new Object())
			.bindedMktGatewayId("test2")
			.build();
	
	@BeforeEach
	void cleanUp(){
		gatewayMgr.findAll().forEach(gw -> {
			gatewayMgr.remove(Identifier.of(gw.getDescription().getGatewayId()));
		});
		repo.deleteAll();
	}

	@Test
	void testCreateGateway() {
		GatewayVO gateway = service.createGateway(gd1);
		assertThat(gateway.getDescription()).isEqualTo(gd1);
		assertThat(gatewayMgr.marketGateways()).hasSize(1);
	}

	@Test
	void testUpdateGateway() {
		service.createGateway(gd1);
		service.updateGateway(gd1.toBuilder().description("add something").build());
		assertThat(gatewayMgr.marketGateways()).hasSize(1);
	}
	
	@Test
	void testUpdateGatewayFail1() {
		service.createGateway(gd1);
		assertThrows(IllegalStateException.class, () -> {
			service.updateGateway(gd2);
		});
	}
	
	@Test
	void testUpdateGatewayFail2() {
		service.createGateway(gd1);
		assertThrows(NoSuchElementException.class, () -> {
			service.updateGateway(gd4);
		});
	}

	@Test
	void testFindAllGateway() {
		service.createGateway(gd1);
		assertThat(gatewayMgr.findAll()).hasSize(1);
	}

	@Test
	void testRemoveGateway() {
		service.createGateway(gd1);
		service.removeGateway(gd1.getGatewayId());
		assertThat(gatewayMgr.findAll()).isEmpty();
	}

	@Test
	void testConnect() {
		service.createGateway(gd1);
		service.connect(gd1.getGatewayId());
		GatewayVO gateway = service.findAllGateway().get(0);
		assertThat(gateway.getConnection()).isEqualTo(ConnectionState.CONNECTED);
	}

	@Test
	void testDisconnect() {
		testConnect();
		service.disconnect(gd1.getGatewayId());
		Gateway gateway = gatewayMgr.get(Identifier.of(gd1.getGatewayId()));
		assertThat(gateway.getConnection()).isEqualTo(ConnectionState.DISCONNECTED);
	}

	@Test
	void testMoneyIO() {
		service.createGateway(gd2);
		assertDoesNotThrow(() -> {
			service.moneyIO(gd2.getGatewayId(), 100);
		});
	}

}
