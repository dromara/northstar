package tech.quantit.northstar.main.restful;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import cn.hutool.crypto.digest.MD5;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.constant.ReturnCode;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.main.NorthstarApplication;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
class ContractControllerTest {

	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session = new MockHttpSession();
	
	@MockBean
	private SocketIOServer socketServer;
	
	@MockBean
	private IGatewayRepository gatewayRepo;
	
	@BeforeEach
	public void setUp() throws Exception {
		when(gatewayRepo.findById(anyString())).thenReturn(GatewayDescription.builder().gatewayUsage(GatewayUsage.MARKET_DATA).build());
		
		long timestamp = System.currentTimeMillis();
		String token = MD5.create().digestHex("123456" + timestamp);
		mockMvc.perform(post("/northstar/auth/login?timestamp="+timestamp).contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new NsUser("admin",token))).session(session))
			.andExpect(status().isOk());
	}
	
	@Test
	void testGetContractList() throws Exception {
		mockMvc.perform(get("/northstar/contracts?channelType=CTP").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	void testGetSubscribableContractList() throws Exception {
		mockMvc.perform(get("/northstar/contracts/subscribed?gatewayId=CTP").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

}
