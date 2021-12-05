package tech.quantit.northstar.main.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.alibaba.fastjson.JSON;
import com.corundumstudio.socketio.SocketIOServer;

import common.TestMongoUtils;
import tech.quantit.northstar.common.constant.ReturnCode;
import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.main.NorthstarApplication;
import tech.quantit.northstar.main.engine.broadcast.SocketIOMessageEngine;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
class DataSyncTest {

	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session;
	
	@MockBean
	private SocketIOMessageEngine msgEngine;
	
	@MockBean
	private SocketIOServer socketServer;
	
	@BeforeEach
	void setUp() throws Exception {
		session = new MockHttpSession();
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(new NsUser("admin","123456"))).session(session))
			.andExpect(status().isOk());
	}
	
	@AfterEach
	void tearDown() throws Exception {
		TestMongoUtils.clearDB();
	}
	
	@Test
	void shouldSync() throws Exception {
		mockMvc.perform(get("/data/sync").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}


	@Test
	void shouldGetHistoryBar() throws Exception {
		mockMvc.perform(get("/data/his/bar?gatewayId=test&unifiedSymbol=rb2201@SHFE@FUTURES&startDate=20210808&endDate=20210810").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	void shouldGetAvailableContract() throws Exception {
		mockMvc.perform(get("/data/contracts").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	
}
