package org.dromara.northstar.app.restful;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.app.service.MetaDataService;
import org.dromara.northstar.strategy.IMessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
@AutoConfigureMockMvc
class MetaDataControllerTest {
	
	@Autowired
	MockMvc mvc;
	
	@MockitoBean
	MetaDataService service;
	
	@MockitoBean
	private IMessageSender sender;

	@Test
	void testAvailableContracts() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/metadata/contracts/SIM"))
			.andExpect(status().isOk());
		
		mvc.perform(MockMvcRequestBuilders.get("/northstar/metadata/contracts/SIM/sub"))
		.andExpect(status().isOk());
	}

	@Test
	void testGatewaySettingsMeta() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/metadata/gateway/settings"))
			.andExpect(status().isOk());
	}

	@Test
	void testChannelsMeta() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/metadata/channels"))
			.andExpect(status().isOk());
	}

	@Test
	void testModuleSettingsMeta() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/metadata/strategy/settings"))
			.andExpect(status().isOk());
	}

}
