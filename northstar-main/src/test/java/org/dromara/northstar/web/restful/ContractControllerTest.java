package org.dromara.northstar.web.restful;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.constant.ReturnCode;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.NsUser;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.event.BroadcastHandler;
import org.junit.jupiter.api.AfterAll;
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
import net.sf.ehcache.CacheManager;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
@AutoConfigureMockMvc
class ContractControllerTest {

	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session = new MockHttpSession();
	
	@MockBean
	private SocketIOServer socketServer;
	
	@MockBean
	private BroadcastHandler bcHandler;
	
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
	
	@AfterAll
	static void clearCache() {
		CacheManager.getInstance().shutdown();
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
