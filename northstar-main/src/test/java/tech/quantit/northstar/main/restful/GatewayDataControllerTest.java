package tech.quantit.northstar.main.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.main.NorthstarApplication;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
class GatewayDataControllerTest {

	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session = new MockHttpSession();
	
	@MockBean
	private SocketIOServer socketServer;
	
	@BeforeEach
	public void setUp() throws Exception {
		mockMvc.perform(post("/northstar/auth/login").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new NsUser("admin","123456"))).session(session))
			.andExpect(status().isOk());
	}
	
	@Test
	void testLoadWeeklyBarData() throws Exception {
		mockMvc.perform(get("/northstar/data/bar/min?gatewayId=testGateway&unifiedSymbol=rb2205@SHFE@FUTURES&firstLoad=true&refStartTimestamp="+System.currentTimeMillis()).session(session))
			.andExpect(status().isOk());
	}

}
