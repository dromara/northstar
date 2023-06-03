package org.dromara.northstar.web.restful;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.util.List;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.constant.ReturnCode;
import org.dromara.northstar.common.model.ComponentAndParamsPair;
import org.dromara.northstar.common.model.ComponentField;
import org.dromara.northstar.common.model.ComponentMetaInfo;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.MockTradeDescription;
import org.dromara.northstar.common.model.ModuleAccountDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.NsUser;
import org.dromara.northstar.data.jdbc.GatewayDescriptionRepository;
import org.dromara.northstar.data.jdbc.ModuleDealRecordRepository;
import org.dromara.northstar.data.jdbc.ModuleDescriptionRepository;
import org.dromara.northstar.data.jdbc.ModuleRuntimeDescriptionRepository;
import org.dromara.northstar.event.BroadcastHandler;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.playback.PlaybackDataServiceManager;
import org.dromara.northstar.gateway.playback.PlaybackGatewayFactory;
import org.dromara.northstar.gateway.playback.PlaybackGatewaySettings;
import org.dromara.northstar.gateway.sim.trade.SimGatewayFactory;
import org.dromara.northstar.gateway.time.GenericTradeTime;
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
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.ContractField;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
class ModuleControllerTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session = new MockHttpSession();
	
	@Autowired
	private ModuleDescriptionRepository mdRepo;
	
	@Autowired
	private ModuleRuntimeDescriptionRepository mrdRepo;
	
	@Autowired
	private ModuleDealRecordRepository mdrRepo;
	
	@Autowired
	private GatewayDescriptionRepository gwRepo;
	
	@MockBean
	private SocketIOServer socketServer;
	
	@MockBean
	private BroadcastHandler bcHandler;
	
	@Autowired
	private SimGatewayFactory simGatewayFactory;
	
	@Autowired
	GatewayMetaProvider gatewayMetaProvider;
	
	@Autowired
	PlaybackGatewayFactory playbackGatewayFactory;
	
	@Autowired
	PlaybackDataServiceManager dsMgr;
	
	ModuleDescription md1;
	
	ModuleDescription md2;
	
	String strategy = "{\"name\":\"示例信号策略\",\"className\":\"org.dromara.northstar.strategy.example.BeginnerSampleStrategy\"}";
	String strategyParams = "{\"label\":\"操作间隔\",\"name\":\"actionInterval\",\"order\":10,\"type\":\"Number\",\"value\":60,\"unit\":\"秒\",\"options\":[]}";
	
	TestFieldFactory factory = new TestFieldFactory("CTP账户");
	
	MockTradeDescription mockTrade = MockTradeDescription.builder()
			.gatewayId("CTP账户")
			.offsetFlag(OffsetFlagEnum.OF_Open)
			.contractId("rb0000@SHFE@FUTURES")
			.direction(DirectionEnum.D_Buy)
			.price(2000)
			.volume(1)
			.build();
	
	@MockBean
	private IMarketCenter mktCenter;
	
	@BeforeEach
	public void setUp() throws Exception {
		gatewayMetaProvider.add(ChannelType.PLAYBACK, new PlaybackGatewaySettings(), playbackGatewayFactory, dsMgr);
		gatewayMetaProvider.add(ChannelType.SIM, null, simGatewayFactory, null);
		
		Contract c = mock(Contract.class);
		when(mktCenter.getContract(any(Identifier.class))).thenReturn(c);
		when(c.contractField()).thenReturn(ContractField.newBuilder().setChannelType("PLAYBACK").setUnifiedSymbol("rb0000@SHFE@FUTURES").build());
		when(c.tradeTimeDefinition()).thenReturn(new GenericTradeTime());
		
		long time = System.currentTimeMillis();
		String token = MD5.create().digestHex("123456" + time);
		mockMvc.perform(post("/northstar/auth/login?timestamp="+time).contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(new NsUser("admin",token))).session(session))
			.andExpect(status().isOk());
		GatewayDescription gatewayDes = TestGatewayFactory.makeMktGateway("PB", ChannelType.PLAYBACK, TestGatewayFactory.makeGatewaySettings(PlaybackGatewaySettings.class),false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes)).session(session));
		
		GatewayDescription gatewayDes2 = TestGatewayFactory.makeTrdGateway("CTP账户", "PB", ChannelType.SIM, TestGatewayFactory.makeGatewaySettings(null), false);
		mockMvc.perform(post("/northstar/gateway").contentType(MediaType.APPLICATION_JSON).content(JSON.toJSONString(gatewayDes2)).session(session));
		
		ComponentAndParamsPair cpp = ComponentAndParamsPair.builder()
				.componentMeta(JSON.parseObject(strategy, ComponentMetaInfo.class))
				.initParams(List.of(JSON.parseObject(strategyParams, ComponentField.class)))
				.build();
		md1 = ModuleDescription.builder()
				.moduleName("testModule")
				.type(ModuleType.SPECULATION)
				.usage(ModuleUsage.PROD)
				.strategySetting(cpp)
				.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
				.moduleAccountSettingsDescription(List.of(ModuleAccountDescription.builder()
						.accountGatewayId("CTP账户")
						.bindedContracts(List.of(ContractSimpleInfo.builder().unifiedSymbol("rb0000@SHFE@FUTURES").value("rb0000@SHFE@FUTURES@CTP").build()))
						.build()))
				.numOfMinPerBar(1)
				.weeksOfDataForPreparation(1)
				.build();
		
		md2 = ModuleDescription.builder()
				.moduleName("testModule")
				.type(ModuleType.SPECULATION)
				.usage(ModuleUsage.PROD)
				.strategySetting(cpp)
				.closingPolicy(ClosingPolicy.FIRST_IN_FIRST_OUT)
				.moduleAccountSettingsDescription(List.of(ModuleAccountDescription.builder()
						.accountGatewayId("CTP账户")
						.bindedContracts(List.of(ContractSimpleInfo.builder().unifiedSymbol("rb0000@SHFE@FUTURES").value("rb0000@SHFE@FUTURES@CTP").build()))
						.build()))
				.numOfMinPerBar(10)
				.weeksOfDataForPreparation(1)
				.build();
	}
	
	@AfterEach
	void cleanUp() {
		mdRepo.deleteAll();
		mrdRepo.deleteAll();
		mdrRepo.deleteAll();
		gwRepo.deleteAll();
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
