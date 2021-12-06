package tech.quantit.northstar.main.restful;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
import tech.quantit.northstar.common.constant.GatewayType;
import tech.quantit.northstar.common.constant.ReturnCode;
import tech.quantit.northstar.common.model.CtpSettings;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.NsUser;
import tech.quantit.northstar.common.model.SimSettings;
import tech.quantit.northstar.main.NorthstarApplication;
import tech.quantit.northstar.main.engine.broadcast.SocketIOMessageEngine;
import tech.quantit.northstar.strategy.api.model.ModulePositionInfo;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
public class ModuleTest {

	private String symbol;
	
	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session;
	
	@MockBean
	private SocketIOMessageEngine msgEngine;
	
	@MockBean
	private SocketIOServer socketServer;
	
	@BeforeEach
	public void setUp() throws Exception {
		LocalDate date = LocalDate.now().plusDays(45);
		String year = date.getYear() % 100 + "";
		String month = String.format("%02d", date.getMonth().getValue());
		symbol = "sim" + year + month;
		
		session = new MockHttpSession();
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(new NsUser("admin","123456"))).session(session))
			.andExpect(status().isOk());
		
		String json = JSON.toJSONString(TestGatewayFactory.makeTrdGateway("TG1", "TG2",  GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class),false));
		mockMvc.perform(post("/mgt/gateway").contentType(MediaType.APPLICATION_JSON_UTF8).content(json).session(session))
			.andExpect(status().isOk());
	}
	
	@AfterEach
	public void tearDown() throws InterruptedException {
		TestMongoUtils.clearDB();
	}
	
	@Test
	public void shouldSuccessfullyCreate() throws Exception {
		String demoStr = "{\"moduleName\":\"TEST\",\"accountGatewayId\":\"TG1\",\"signalPolicy\":{\"componentMeta\":{\"name\":\"示例信号策略\",\"className\":\"tech.quantit.northstar.strategy.api.policy.signal.SampleSignalPolicy\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"长周期\",\"name\":\"actionInterval\",\"order\":30,\"type\":\"Number\",\"value\":\"3\",\"unit\":\"秒\",\"options\":[]}]},\"riskControlRules\":[{\"componentMeta\":{\"name\":\"委托超时限制\",\"className\":\"tech.quantit.northstar.strategy.api.policy.risk.TimeExceededRule\"},\"initParams\":[{\"label\":\"超时时间\",\"name\":\"timeoutSeconds\",\"order\":0,\"type\":\"Number\",\"value\":\"23\",\"unit\":\"秒\",\"options\":[]}]}],\"dealer\":{\"componentMeta\":{\"name\":\"示例交易策略\",\"className\":\"tech.quantit.northstar.strategy.api.policy.dealer.SampleDealer\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"开仓手数\",\"name\":\"openVol\",\"order\":20,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"\",\"options\":[]},{\"label\":\"价格类型\",\"name\":\"openPriceTypeStr\",\"order\":30,\"type\":\"Options\",\"value\":\"市价\",\"unit\":\"\",\"options\":[\"对手价\",\"市价\",\"最新价\",\"排队价\",\"信号价\"]},{\"label\":\"超价\",\"name\":\"overprice\",\"order\":40,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"Tick\",\"options\":[]}]},\"enabled\":false,\"type\":\"CTA\"}";
		
		mockMvc.perform(post("/module").contentType(MediaType.APPLICATION_JSON_UTF8).content(demoStr).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldSuccessfullyUpdate() throws Exception {
		shouldSuccessfullyCreate();
		String demoStr = "{\"moduleName\":\"TEST\",\"accountGatewayId\":\"TG1\",\"signalPolicy\":{\"componentMeta\":{\"name\":\"示例信号策略\",\"className\":\"tech.quantit.northstar.strategy.api.policy.signal.SampleSignalPolicy\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"长周期\",\"name\":\"actionInterval\",\"order\":30,\"type\":\"Number\",\"value\":\"3\",\"unit\":\"秒\",\"options\":[]}]},\"riskControlRules\":[{\"componentMeta\":{\"name\":\"委托超时限制\",\"className\":\"tech.quantit.northstar.strategy.api.policy.risk.TimeExceededRule\"},\"initParams\":[{\"label\":\"超时时间\",\"name\":\"timeoutSeconds\",\"order\":0,\"type\":\"Number\",\"value\":\"23\",\"unit\":\"秒\",\"options\":[]}]}],\"dealer\":{\"componentMeta\":{\"name\":\"示例交易策略\",\"className\":\"tech.quantit.northstar.strategy.api.policy.dealer.SampleDealer\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"开仓手数\",\"name\":\"openVol\",\"order\":20,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"\",\"options\":[]},{\"label\":\"价格类型\",\"name\":\"openPriceTypeStr\",\"order\":30,\"type\":\"Options\",\"value\":\"市价\",\"unit\":\"\",\"options\":[\"对手价\",\"市价\",\"最新价\",\"排队价\",\"信号价\"]},{\"label\":\"超价\",\"name\":\"overprice\",\"order\":40,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"Tick\",\"options\":[]}]},\"enabled\":true,\"type\":\"CTA\"}";
		
		mockMvc.perform(put("/module").contentType(MediaType.APPLICATION_JSON_UTF8).content(demoStr).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldFindModules() throws Exception {
		mockMvc.perform(get("/module").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldSuccessfullyRemove() throws Exception {
		shouldSuccessfullyCreate();
		
		mockMvc.perform(delete("/module?name=TEST").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldCreateModulePosition() throws Exception {
		prepareGateway();
		System.out.println("等待gateway初始化");
		Thread.sleep(2000);
		shouldSuccessfullyCreate();
		ModulePositionInfo position = ModulePositionInfo.builder()
				.unifiedSymbol(symbol+"@SHFE@FUTURES")
				.multiplier(10)
				.openTime(System.currentTimeMillis())
				.openPrice(1234)
				.openTradingDay("20210609")
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		
		mockMvc.perform(post("/module/TEST/position").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(position)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldUpdateModulePosition() throws Exception {
		prepareGateway();
		shouldSuccessfullyCreate();
		ModulePositionInfo position = ModulePositionInfo.builder()
				.unifiedSymbol(symbol+"@SHFE@FUTURES")
				.multiplier(10)
				.openTime(System.currentTimeMillis())
				.openPrice(1234)
				.openTradingDay("20210609")
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		
		mockMvc.perform(put("/module/TEST/position").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(position)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldRemoveModulePosition() throws Exception {
		shouldCreateModulePosition();
		
		mockMvc.perform(delete("/module/TEST/position?unifiedSymbol="+symbol+"@SHFE@FUTURES&dir=PD_Long").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	private void prepareGateway() throws Exception {
		GatewayDescription gateway = TestGatewayFactory.makeMktGateway("TG5", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class),false);
		mockMvc.perform(post("/mgt/gateway").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(gateway)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
		
		GatewayDescription gateway2 = TestGatewayFactory.makeTrdGateway("TG5Account", "TG5", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class),true);
		mockMvc.perform(post("/mgt/gateway").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(gateway2)).session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
		
		Thread.sleep(1000);
	}
	
	@Test
	public void shouldSuccessfullyGetModuleCurrentPerformance() throws Exception {
		shouldCreateModulePosition();
		mockMvc.perform(get("/module/info?name=TEST").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldSuccessfullyGetModuleDealRecords() throws Exception {
		shouldCreateModulePosition();
		
		mockMvc.perform(get("/module/records?name=TEST").session(session))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldSuccessfullyGetModuleTradeRecords() throws Exception {
		shouldCreateModulePosition();
		
		mockMvc.perform(get("/module/records/trade?name=TEST").session(session))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldSuccessfullyToggleModuleState() throws Exception {
		shouldCreateModulePosition();
		
		mockMvc.perform(get("/module/toggle?name=TEST").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	
}
