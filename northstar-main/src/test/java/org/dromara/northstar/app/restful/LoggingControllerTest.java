package org.dromara.northstar.app.restful;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.app.service.LoggingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
@AutoConfigureMockMvc
class LoggingControllerTest {

	@Autowired
	MockMvc mvc;
	
	@MockitoBean
	LoggingService service;
	
	@Test
	void testTailLong() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/logging/tail/100"))
			.andExpect(status().isOk());
	}

	@Test
	void testTailStringLong() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/logging/tail/100/module"))
			.andExpect(status().isOk());
	}

	@Test
	void testSetLogLevelLevel() throws Exception {
		mvc.perform(MockMvcRequestBuilders.put("/northstar/logging/setLevel/INFO"))
			.andExpect(status().isOk());
	}

	@Test
	void testSetLogLevelStringLevel() throws Exception {
		mvc.perform(MockMvcRequestBuilders.put("/northstar/logging/setLevel/INFO/module"))
			.andExpect(status().isOk());
	}

}
