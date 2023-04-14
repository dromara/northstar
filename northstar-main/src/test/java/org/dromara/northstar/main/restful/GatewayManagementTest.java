package org.dromara.northstar.main.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.main.handler.broadcast.SocketIOMessageEngine;
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

import cn.hutool.crypto.digest.MD5;
import common.TestGatewayFactory;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.constant.GatewayUsage;
import tech.quantit.northstar.common.constant.ReturnCode;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.gateway.ctp.CtpGatewaySettings;

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
	
	@Autowired
	private RedisTemplate<String, byte[]> redisTemplate;
	
	@BeforeEach
	public void setUp() throws Exception {
		session = new MockHttpSession();
		long time = System.currentTimeMillis();
		String token = MD5.create().digestHex("123456" + time);
		mockMvc.perform(post("/northstar/auth/login?timestamp="+time).contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new NsUser("admin",token))).session(session))
			.andExpect(status().isOk());
	}
	
	@AfterEach
	public void tearDown() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}
	
	@Test
	public void shouldFailWithoutAuth() throws Exception {
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("testGateway", ChannelType.CTP, TestGatewayFactory.makeGatewaySettings(CtpGatewaySettings.class), false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes)))
			.andExpect(status().is(401));
	}

	@Test
	public void shouldCreateGateway() throws Exception {
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("CTP", ChannelType.CTP, TestGatewayFactory.makeGatewaySettings(CtpGatewaySettings.class),false);
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
		
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("CTP", ChannelType.CTP, TestGatewayFactory.makeGatewaySettings(CtpGatewaySettings.class), true);
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
		GatewayDescription gwDes = TestGatewayFactory.makeMktGateway("CTP", ChannelType.CTP, null,false);
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
		
		GatewayDescription gwDes = TestGatewayFactory.makeTrdGateway("TG2", "", ChannelType.SIM, new Object(), false);
		
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
	public void shouldGetGatewayTypeOptions() throws Exception {
		mockMvc.perform(get("/northstar/gateway/types").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldGetGatewaySettings() throws Exception {
		mockMvc.perform(get("/northstar/gateway/settings?channelType=CTP").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
}
