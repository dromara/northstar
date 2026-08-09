package org.dromara.northstar.app.restful;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.app.service.GatewayService;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.strategy.IMessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.alibaba.fastjson2.JSON;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
@AutoConfigureMockMvc
class GatewayControllerTest {

	@Autowired
	MockMvc mvc;
	
	@MockitoBean
	GatewayService service;
	
	@MockitoBean
	private IMessageSender sender;
	
	GatewayDescription gd = GatewayDescription.builder().build();
	
	@Test
	void testCreateGateway() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/northstar/gateway/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(gd)))
			.andExpect(status().isOk());
	}

	@Test
	void testUpdateGateway() throws Exception {
		mvc.perform(MockMvcRequestBuilders.put("/northstar/gateway/update")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(gd)))
			.andExpect(status().isOk());
	}

	@Test
	void testRemoveGateway() throws Exception {
		mvc.perform(MockMvcRequestBuilders.delete("/northstar/gateway/remove/test"))
			.andExpect(status().isOk());
	}

	@Test
	void testFindAllGateway() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/gateway/all"))
			.andExpect(status().isOk());
	}

	@Test
	void testConnect() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/gateway/connect/test"))
			.andExpect(status().isOk());
	}

	@Test
	void testDisconnect() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/gateway/disconnect/test"))
			.andExpect(status().isOk());
	}

	@Test
	void testMoneyIO() throws Exception {
		mvc.perform(MockMvcRequestBuilders.put("/northstar/gateway/deposit/test?amount=-1"))
			.andExpect(status().isOk());
	}

}
