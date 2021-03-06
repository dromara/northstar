package tech.quantit.northstar.main.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.util.List;

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
import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.ModuleType;
import tech.quantit.northstar.common.constant.ReturnCode;
import tech.quantit.northstar.common.model.ComponentAndParamsPair;
import tech.quantit.northstar.common.model.ComponentField;
import tech.quantit.northstar.common.model.ComponentMetaInfo;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.MockTradeDescription;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.main.NorthstarApplication;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
class ModuleControllerTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session = new MockHttpSession();
	
	@MockBean
	private SocketIOServer socketServer;
	
	@Autowired
	private RedisTemplate<String, byte[]> redisTemplate;
	
	ModuleDescription md1;
	
	ModuleDescription md2;
	
	String strategy = "{\"name\":\"??????????????????\",\"className\":\"tech.quantit.northstar.strategy.api.demo.BeginnerSampleStrategy\"}";
	String strategyParams = "{\"label\":\"????????????\",\"name\":\"actionInterval\",\"order\":10,\"type\":\"Number\",\"value\":60,\"unit\":\"?????\",\"options\":[]}";
	
	TestFieldFactory factory = new TestFieldFactory("CTP??????");
	
	MockTradeDescription mockTrade = MockTradeDescription.builder()
			.gatewayId("CTP??????")
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.unifiedSymbol("rb2210@SHFE@FUTURES")
			.direction(DirectionEnum.D_Buy)
			.price(2000)
			.volume(1)
			.build();
	
	@Autowired
	private ContractManager contractMgr;
	
	@BeforeEach
	public void setUp() throws Exception {
		contractMgr.addContract(factory.makeContract("rb2210"));
		
		mockMvc.perform(post("/northstar/auth/login").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new NsUser("admin","123456"))).session(session))
			.andExpect(status().isOk());
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("CTP", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class),false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes)).session(session));
		
		GatewayDescription gatewayDes2 = TestGatewayFactory.makeTrdGateway("CTP??????", "CTP", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class),false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes2)).session(session));
		
		ComponentAndParamsPair cpp = ComponentAndParamsPair.builder()
				.componentMeta(JSON.parseObject(strategy, ComponentMetaInfo.class))
				.initParams(List.of(JSON.parseObject(strategyParams, ComponentField.class)))
				.build();
		md1 = ModuleDescription.builder()
				.moduleName("testModule")
				.type(ModuleType.SPECULATION)
				.strategySetting(cpp)
				.closingPolicy(ClosingPolicy.FIFO)
				.moduleAccountSettingsDescription(List.of(ModuleAccountDescription.builder()
						.accountGatewayId("CTP??????")
						.moduleAccountInitBalance(10000)
						.bindedUnifiedSymbols(List.of("rb2210@SHFE@FUTURES"))
						.build()))
				.numOfMinPerBar(1)
				.daysOfDataForPreparation(1)
				.build();
		
		md2 = ModuleDescription.builder()
				.moduleName("testModule")
				.type(ModuleType.SPECULATION)
				.strategySetting(cpp)
				.closingPolicy(ClosingPolicy.FIFO)
				.moduleAccountSettingsDescription(List.of(ModuleAccountDescription.builder()
						.accountGatewayId("CTP??????")
						.moduleAccountInitBalance(10000)
						.bindedUnifiedSymbols(List.of("rb2210@SHFE@FUTURES"))
						.build()))
				.numOfMinPerBar(10)
				.daysOfDataForPreparation(1)
				.build();
	}
	
	@AfterEach
	public void tearDown() {
		redisTemplate.delete(redisTemplate.keys("*"));
	}
	
	@Test
	void testGetRegisteredTradeStrategies() throws Exception {
		mockMvc.perform(get("/northstar/module/strategies").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data").isArray());
	}

	@Test
	void testGetComponentParams() throws Exception {
		mockMvc.perform(post("/northstar/module/strategy/params").contentType(MediaType.APPLICATION_JSON).characterEncoding("UTF-8").content(strategy).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	void testCreateModule() throws UnsupportedEncodingException, Exception {
		mockMvc.perform(post("/northstar/module").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(md1)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	void testUpdateModule() throws UnsupportedEncodingException, Exception {
		testCreateModule();
		
		mockMvc.perform(put("/northstar/module").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(md2)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	void testGetAllModules() throws UnsupportedEncodingException, Exception {
		testCreateModule();
		
		mockMvc.perform(get("/northstar/module").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS))
			.andExpect(jsonPath("$.data").isNotEmpty());
	}

	@Test
	void testRemoveModule() throws UnsupportedEncodingException, Exception {
		testCreateModule();
		
		mockMvc.perform(delete("/northstar/module?name=testModule").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	void testToggleModuleState() throws UnsupportedEncodingException, Exception {
		testCreateModule();
		
		mockMvc.perform(get("/northstar/module/toggle?name=testModule").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
		
		mockMvc.perform(get("/northstar/module/toggle?name=testModule").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	void testGetModuleRealTimeInfo() throws UnsupportedEncodingException, Exception {
		testCreateModule();
		mockMvc.perform(get("/northstar/module/rt/info?name=testModule").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	void testGetDealRecords() throws UnsupportedEncodingException, Exception {
		testCreateModule();
		mockMvc.perform(get("/northstar/module/deal/records?name=testModule").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	void testMockTradeAdjustment() throws UnsupportedEncodingException, Exception {
		testCreateModule();
		
		mockMvc.perform(post("/northstar/module/testModule/mockTrade").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONBytes(mockTrade)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

}
