package org.dromara.northstar.web.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.constant.ReturnCode;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.NsUser;
import org.dromara.northstar.data.jdbc.GatewayDescriptionRepository;
import org.dromara.northstar.event.BroadcastHandler;
import org.dromara.northstar.gateway.playback.PlaybackGatewaySettings;
import org.dromara.northstar.strategy.IMessageSender;
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

import cn.hutool.crypto.digest.MD5;
import common.TestGatewayFactory;

/**
 * GatewayManagement接口黑盒测试类
 * @author KevinHuangwl
 *
 */
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
@AutoConfigureMockMvc
class GatewayManagementTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session;
	
	@Autowired
	private GatewayDescriptionRepository repo;
	
	@MockBean
	private SocketIOServer socketServer;
	
	@MockBean
	private IMessageSender sender;
	
	@MockBean
	private BroadcastHandler bcHandler;
	
	@BeforeEach
	void setUp() throws Exception {
		session = new MockHttpSession();
		long time = System.currentTimeMillis();
		String token = MD5.create().digestHex("123456" + time);
		mockMvc.perform(post("/northstar/auth/login?timestamp="+time).contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new NsUser("admin",token))).session(session))
			.andExpect(status().isOk());
	}
	
	@AfterEach
	void cleanUp(){
		repo.deleteAll();
	}
	
	@Test
	void shouldFailWithoutAuth() throws Exception {
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("testGateway", ChannelType.PLAYBACK, TestGatewayFactory.makeGatewaySettings(PlaybackGatewaySettings.class), false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes)))
			.andExpect(status().is(401));
	}

	@Test
	void shouldCreateGateway() throws Exception {
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("PB", ChannelType.PLAYBACK, TestGatewayFactory.makeGatewaySettings(PlaybackGatewaySettings.class),false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	void shouldFindCreatedGateway() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(get("/northstar/gateway?usage=" + GatewayUsage.MARKET_DATA).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	void shouldUpdateGateway() throws Exception {
		shouldCreateGateway();
		
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("PB", ChannelType.PLAYBACK, TestGatewayFactory.makeGatewaySettings(PlaybackGatewaySettings.class), true);
		mockMvc.perform(put("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	void shouldRemoveGateway() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(delete("/northstar/gateway?gatewayId=PB").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	void shouldFailIfNotProvidingSetting() throws Exception {
		GatewayDescription gwDes = TestGatewayFactory.makeMktGateway("CTP", ChannelType.CTP, null,false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gwDes)).session(session))
			.andExpect(status().is5xxServerError())
			.andExpect(jsonPath("$.status").value(ReturnCode.ERROR));
	}
	
	@Test
	void shouldSuccessWhenGettingActiveState() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(get("/northstar/gateway/active?gatewayId=PB").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	void shouldSuccessWhenConnecting() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(get("/northstar/gateway/connection?gatewayId=PB").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	void shouldSuccessWhenDisconnecting() throws Exception {
		shouldSuccessWhenConnecting();
		
		mockMvc.perform(delete("/northstar/gateway/connection?gatewayId=PB").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	void shouldFailIfGatewayNotFound() throws Exception {
		mockMvc.perform(get("/northstar/gateway/connection?gatewayId=ANY").session(session))
			.andExpect(status().is5xxServerError())
			.andExpect(jsonPath("$.status").value(ReturnCode.NO_SUCH_ELEMENT_EXCEPTION));
		
		mockMvc.perform(delete("/northstar/gateway/connection?gatewayId=ANY").session(session))
			.andExpect(status().is5xxServerError())
			.andExpect(jsonPath("$.status").value(ReturnCode.NO_SUCH_ELEMENT_EXCEPTION));
	}
	
	@Test
	void shouldIncreaseBalance() throws Exception {
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
	void shouldDecreaseBalance() throws Exception {
		shouldIncreaseBalance();
		
		mockMvc.perform(post("/northstar/gateway/moneyio?gatewayId=TG2&money=-10000").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	void shouldFailIfNotSimGateway() throws Exception {
		shouldCreateGateway();
		
		mockMvc.perform(post("/northstar/gateway/moneyio?gatewayId=PB&money=10000").session(session))
			.andExpect(status().is5xxServerError())
			.andExpect(jsonPath("$.status").value(ReturnCode.ERROR));
	}
	
	@Test
	void shouldGetGatewayTypeOptions() throws Exception {
		mockMvc.perform(get("/northstar/gateway/types").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	void shouldGetGatewaySettings() throws Exception {
		mockMvc.perform(get("/northstar/gateway/settings?channelType=PLAYBACK").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
}
