package org.dromara.northstar.web.restful;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.NsUser;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.event.BroadcastHandler;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.strategy.IMessageSender;
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
class GatewayDataControllerTest {

	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session = new MockHttpSession();
	
	@MockBean
	private SocketIOServer socketServer;
	
	@MockBean
	private IMarketCenter contractMgr;
	
	@MockBean
	private IMessageSender sender;
	
	@MockBean
	private IGatewayRepository gatewayRepo;
	
	@MockBean
	private BroadcastHandler bcHandler;
	
	@BeforeEach
	public void setUp() throws Exception {
		IContract contract = mock(IContract.class);
		when(contractMgr.getContract(any(), anyString())).thenReturn(contract);
		when(contract.contract()).thenReturn(Contract.builder().channelType(ChannelType.PLAYBACK).unifiedSymbol("rb2205@SHFE@FUTURES").build());
		
		when(gatewayRepo.findById(anyString())).thenReturn(GatewayDescription.builder().channelType(ChannelType.PLAYBACK).build());
		
		long time = System.currentTimeMillis();
		String token = MD5.create().digestHex("123456" + time);
		mockMvc.perform(post("/northstar/auth/login?timestamp="+time).contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new NsUser("admin",token))).session(session))
			.andExpect(status().isOk());
	}
	
	@AfterAll
	static void clearCache() {
		CacheManager.getInstance().shutdown();
	}
	
	@Test
	void testLoadWeeklyBarData() throws Exception {
		mockMvc.perform(get("/northstar/data/bar/min?gatewayId=OTHER&unifiedSymbol=rb2205@SHFE@FUTURES&firstLoad=true&refStartTimestamp="+System.currentTimeMillis()).session(session))
			.andExpect(status().isOk());
	}

}
