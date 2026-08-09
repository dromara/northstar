package org.dromara.northstar.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.strategy.IMessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
class MetaDataServiceTest {
	
	@MockitoBean
	IMessageSender sender;
	
	@Autowired
	MetaDataService service;

	@Test
	void testAvailableContracts() {
		assertThat(service.availableContracts(ChannelType.SIM, "")).isNotEmpty();
		assertThat(service.availableContracts(ChannelType.SIM, "FUT")).isNotEmpty();
		assertThat(service.availableContracts(ChannelType.SIM, "模拟")).isNotEmpty();
	}

	@Test
	void testGatewaySettingsMeta() {
		assertThat(service.gatewaySettingsMeta()).isNotEmpty();
	}

	@Test
	void testModuleSettingsMeta() {
		assertThat(service.strategySettingsMeta()).isNotEmpty();
	}

	@Test
	void testChannelsMeta() {
		assertThat(service.channelsMeta()).isNotEmpty();
	}
	
}
