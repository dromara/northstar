package tech.quantit.northstar.main.restful;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

import cn.hutool.crypto.digest.MD5;
import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.gateway.api.IMarketCenter;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.main.NorthstarApplication;
import xyz.redtorch.pb.CoreField.ContractField;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
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
	private IGatewayRepository gatewayRepo;
	
	@BeforeEach
	public void setUp() throws Exception {
		Contract contract = mock(Contract.class);
		when(contractMgr.getContract(anyString(), anyString())).thenReturn(contract);
		when(contract.contractField()).thenReturn(ContractField.newBuilder().setUnifiedSymbol("rb2205@SHFE@FUTURES").build());
		
		when(gatewayRepo.findById(anyString())).thenReturn(GatewayDescription.builder().channelType(ChannelType.CTP).build());
		
		long time = System.currentTimeMillis();
		String token = MD5.create().digestHex("123456" + time);
		mockMvc.perform(post("/northstar/auth/login?timestamp="+time).contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new NsUser("admin",token))).session(session))
			.andExpect(status().isOk());
		
	}
	
	@Test
	void testLoadWeeklyBarData() throws Exception {
		mockMvc.perform(get("/northstar/data/bar/min?gatewayId=testGateway&unifiedSymbol=rb2205@SHFE@FUTURES&firstLoad=true&refStartTimestamp="+System.currentTimeMillis()).session(session))
			.andExpect(status().isOk());
	}

}
