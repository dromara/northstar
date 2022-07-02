package tech.quantit.northstar.main.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.alibaba.fastjson.JSON;
import com.corundumstudio.socketio.SocketIOServer;

import common.TestGatewayFactory;
import tech.quantit.northstar.common.MessageHandler;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.constant.ReturnCode;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.main.NorthstarApplication;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;

/**
 * GatewayManagement接口黑盒测试类
 * @author KevinHuangwl
 *
 */
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
public class GatewayManagementTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session;
	
	@MockBean
	private SocketIOMessageEngine msgEngine;
	
	@MockBean
	private SocketIOServer socketServer;
	
	@MockBean
	private MessageHandler msgHandler;
	
	@Autowired
	private RedisTemplate<String, byte[]> redisTemplate;
	
	@BeforeEach
	public void setUp() throws Exception {
		session = new MockHttpSession();
		mockMvc.perform(post("/northstar/auth/login").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new NsUser("admin","123456"))).session(session))
			.andExpect(status().isOk());
	}
	
	@AfterEach
	public void tearDown() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}
	
	@Test
	public void shouldFailWithoutAuth() throws Exception {
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("testGateway", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class), false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes)))
			.andExpect(status().is(401));
	}

	@Test
	public void shouldCreateGateway() throws Exception {
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("CTP", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class),false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldFindCreatedGateway() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(get("/northstar/gateway?usage=" + GatewayUsage.MARKET_DATA).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	public void shouldUpdateGateway() throws Exception {
		shouldCreateGateway();
		
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("CTP", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class), true);
		mockMvc.perform(put("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldRemoveGateway() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(delete("/northstar/gateway?gatewayId=CTP").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldFailIfNotProvidingSetting() throws Exception {
		GatewayDescription gwDes = TestGatewayFactory.makeMktGateway("CTP", GatewayType.CTP, null,false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gwDes)).session(session))
			.andExpect(status().is5xxServerError())
			.andExpect(jsonPath("$.status").value(ReturnCode.ERROR));
	}
	
	@Test
	public void shouldSuccessWhenGettingActiveState() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(get("/northstar/gateway/active?gatewayId=CTP").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldSuccessWhenConnecting() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(get("/northstar/gateway/connection?gatewayId=CTP").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldSuccessWhenDisconnecting() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(delete("/northstar/gateway/connection?gatewayId=CTP").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldFailIfGatewayNotFound() throws Exception {
		mockMvc.perform(get("/northstar/gateway/connection?gatewayId=ANY").session(session))
			.andExpect(status().is5xxServerError())
			.andExpect(jsonPath("$.status").value(ReturnCode.NO_SUCH_ELEMENT_EXCEPTION));
		
		mockMvc.perform(delete("/northstar/gateway/connection?gatewayId=ANY").session(session))
			.andExpect(status().is5xxServerError())
			.andExpect(jsonPath("$.status").value(ReturnCode.NO_SUCH_ELEMENT_EXCEPTION));
	}
	
	@Test
	public void shouldIncreaseBalance() throws Exception {
		shouldCreateGateway();
		
		GatewayDescription gwDes = TestGatewayFactory.makeTrdGateway("TG2", "", GatewayType.SIM, new Object(), false);
		
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gwDes)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
		
		mockMvc.perform(post("/northstar/gateway/moneyio?gatewayId=TG2&money=10000").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldDecreaseBalance() throws Exception {
		shouldIncreaseBalance();
		
		mockMvc.perform(post("/northstar/gateway/moneyio?gatewayId=TG2&money=-10000").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldFailIfNotSimGateway() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(post("/northstar/gateway/moneyio?gatewayId=CTP&money=10000").session(session))
			.andExpect(status().is5xxServerError())
			.andExpect(jsonPath("$.status").value(ReturnCode.ERROR));
	}
	
	@Test
	public void shouldGetContractDef() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(get("/northstar/gateway/contractDefs").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());
	}
	
	@Test
	public void shouldGetSubscribableContracts() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(get("/northstar/gateway/subContracts?gatewayId=CTP").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());
	}
}
