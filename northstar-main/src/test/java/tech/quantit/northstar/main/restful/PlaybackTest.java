package tech.quantit.northstar.main.restful;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

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

import common.TestGatewayFactory;
import common.TestMongoUtils;
import tech.quantit.northstar.common.MessageHandler;
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.constant.ReturnCode;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.gateway.sim.persistence.SimAccountRepository;
import tech.quantit.northstar.main.NorthstarApplication;
import tech.quantit.northstar.main.handler.broadcast.SocketIOMessageEngine;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.ModuleRepository;
import tech.quantit.northstar.strategy.api.model.ModuleInfo;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.ContractField;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
public class PlaybackTest {
	
	private String symbol;

	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session;
	
	@MockBean
	private SocketIOMessageEngine msgEngine;
	
	@MockBean
	private SocketIOServer socketServer;
	
	@MockBean
	private ModuleRepository moduleRepo;
	
	@MockBean
	private SimAccountRepository simAccountRepo;
	
	@MockBean
	private MarketDataRepository mdRepo;
	
	@MockBean
	private ContractManager contractMgr;
	
	@MockBean
	private MessageHandler msgHandler;
	
	TestFieldFactory factory = new TestFieldFactory("test");
	
	@BeforeEach
	public void setUp() throws Exception {
		LocalDate date = LocalDate.now().plusDays(45);
		String year = date.getYear() % 100 + "";
		String month = String.format("%02d", date.getMonth().getValue());
		symbol = "sim" + year + month;
		ContractField contract = factory.makeContract(symbol + "@SHFE@FUTURES");
		when(contractMgr.getContract(symbol + "@SHFE@FUTURES")).thenReturn(contract, contract, contract, contract, contract);
		
		session = new MockHttpSession();
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(new NsUser("admin","123456"))).session(session))
			.andExpect(status().isOk());
		
		String json = JSON.toJSONString(TestGatewayFactory.makeTrdGateway("TG1", "TG2",  GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class),false));
		mockMvc.perform(post("/mgt/gateway").contentType(MediaType.APPLICATION_JSON_UTF8).content(json).session(session))
			.andExpect(status().isOk());
		
		String demoStr = "{\"moduleName\":\"TEST\",\"accountGatewayId\":\"TG1\",\"signalPolicy\":{\"componentMeta\":{\"name\":\"示例策略\",\"className\":\"tech.quantit.northstar.strategy.api.policy.signal.SampleSignalPolicy\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\"" + symbol + "@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"长周期\",\"name\":\"actionInterval\",\"order\":30,\"type\":\"Number\",\"value\":\"3\",\"unit\":\"秒\",\"options\":[]}]},\"riskControlRules\":[{\"componentMeta\":{\"name\":\"委托超时限制\",\"className\":\"tech.quantit.northstar.strategy.api.policy.risk.TimeExceededRule\"},\"initParams\":[{\"label\":\"超时时间\",\"name\":\"timeoutSeconds\",\"order\":0,\"type\":\"Number\",\"value\":\"23\",\"unit\":\"秒\",\"options\":[]}]}],\"dealer\":{\"componentMeta\":{\"name\":\"示例交易策略\",\"className\":\"tech.quantit.northstar.strategy.api.policy.dealer.SampleDealer\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\"" + symbol + "@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"开仓手数\",\"name\":\"openVol\",\"order\":20,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"\",\"options\":[]},{\"label\":\"价格类型\",\"name\":\"openPriceTypeStr\",\"order\":30,\"type\":\"Options\",\"value\":\"市价\",\"unit\":\"\",\"options\":[\"对手价\",\"市价\",\"最新价\",\"排队价\",\"信号价\"]}]},\"enabled\":false,\"type\":\"CTA\"}";
		mockMvc.perform(post("/module").contentType(MediaType.APPLICATION_JSON_UTF8).content(demoStr).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
		
		when(mdRepo.loadDataByDate(anyString(), anyString(), anyString())).thenReturn(Collections.EMPTY_LIST);
		
		String moduleStr = "{\"moduleName\":\"TESTM\",\"signalPolicy\":{\"componentMeta\":{\"name\":\"示例策略\",\"className\":\"tech.quantit.northstar.strategy.api.policy.signal.SampleSignalPolicy\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\"" + symbol+ "@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"操作间隔\",\"name\":\"actionInterval\",\"order\":20,\"type\":\"Number\",\"value\":\"600\",\"unit\":\"秒\",\"options\":[]}]},\"riskControlRules\":[],\"dealer\":{\"componentMeta\":{\"name\":\"示例交易策略\",\"className\":\"tech.quantit.northstar.strategy.api.policy.dealer.SampleDealer\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\"" + symbol + "@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"开仓手数\",\"name\":\"openVol\",\"order\":20,\"type\":\"Number\",\"value\":\"1\",\"unit\":\"\",\"options\":[]},{\"label\":\"开仓价格类型\",\"name\":\"openPriceTypeStr\",\"order\":30,\"type\":\"Options\",\"value\":\"市价\",\"unit\":\"\",\"options\":[\"对手价\",\"市价\",\"最新价\",\"排队价\",\"信号价\"]}]},\"accountGatewayId\":\"SIM账户\",\"enabled\":false,\"type\":\"CTA\",\"numOfDaysOfDataRef\":0}";
		when(moduleRepo.findModuleInfo(anyString())).thenReturn(JSON.parseObject(moduleStr, ModuleInfo.class));
		
		
	}
	
	@AfterEach
	public void tearDown() throws InterruptedException {
		TestMongoUtils.clearDB();
	}
	
	@Test
	public void shouldSuccessfullyPlay() throws Exception {
		PlaybackDescription playbackDescription = PlaybackDescription.builder()
				.startDate("20211111")
				.endDate("20211122")
				.moduleNames(List.of("TEST"))
				.playbackAccountInitialBalance(100000)
				.precision(PlaybackPrecision.TICK)
				.build();
		
		mockMvc.perform(post("/pb/play").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(playbackDescription)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
		
	}

	@Test
	public void shouldGetPlayProcess() throws Exception {
		shouldSuccessfullyPlay();
		
		mockMvc.perform(get("/pb/play/process").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldThrowIfNotPlayAndGetTheProcess() throws Exception {
		mockMvc.perform(get("/pb/play/process").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.ERROR));
	}

	@Test
	public void shouldThrowIfNotPlayAndGetTheBalance() throws Exception {
		mockMvc.perform(get("/pb/balance?moduleName=TESTM").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.ERROR));
	}
	
	@Test
	public void shouldGetPlaybackBalance() throws Exception {
		shouldSuccessfullyPlay();
		
		mockMvc.perform(get("/pb/balance?moduleName=TESTM").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}

	@Test
	public void shouldGetReadyStateIfNotPlay() throws Exception {
		Thread.sleep(500);
		mockMvc.perform(get("/pb/readiness").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS))
			.andExpect(jsonPath("$.data").value(true));
	}
	
}
