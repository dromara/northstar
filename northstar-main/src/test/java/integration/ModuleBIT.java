package integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import common.TestGatewayFactory;
import common.TestMongoUtils;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.NsUser;
import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.common.model.SimSettings;
import tech.xuanwu.northstar.strategy.common.model.ModulePosition;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

public class ModuleBIT {

	private String cookie;
	
	private RestTemplate rest = new RestTemplateBuilder().rootUri("http://localhost:8888/northstar").build();
	
	private HttpHeaders header;
	
	private String symbol;
	
	@Before
	public void setUp() throws InterruptedException {
		LocalDate date = LocalDate.now().plusDays(45);
		String year = date.getYear() % 100 + "";
		String month = String.format("%02d", date.getMonth().getValue());
		symbol = "ni" + year + month;
		
		ResponseEntity<ResultBean> result = rest.postForEntity("/auth/login", new NsUser("admin","123456"), ResultBean.class);
		cookie = result.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
		header = new HttpHeaders();
		header.add("Cookie", cookie);
		header.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
		
		HttpEntity<GatewayDescription> entity = new HttpEntity<GatewayDescription>(
				TestGatewayFactory.makeTrdGateway("TG1", "TG2",  GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class),false),
				HttpHeaders.readOnlyHttpHeaders(header));
		rest.postForObject("/mgt/gateway", entity, ResultBean.class);
		
	}
	
	@After
	public void tearDown() throws InterruptedException {
		TestMongoUtils.clearDB();
	}
	
	@Test
	public void shouldSuccessfullyCreate() {
		String demoStr = "{\"moduleName\":\"TEST\",\"accountGatewayId\":\"TG1\",\"signalPolicy\":{\"componentMeta\":{\"name\":\"示例策略\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.signal.SampleSignalPolicy\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"短周期\",\"name\":\"shortPeriod\",\"order\":20,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"天\",\"options\":[]},{\"label\":\"长周期\",\"name\":\"longPeriod\",\"order\":30,\"type\":\"Number\",\"value\":\"3\",\"unit\":\"天\",\"options\":[]}]},\"riskControlRules\":[{\"componentMeta\":{\"name\":\"委托超时限制\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.risk.TimeExceededRule\"},\"initParams\":[{\"label\":\"超时时间\",\"name\":\"timeoutSeconds\",\"order\":0,\"type\":\"Number\",\"value\":\"23\",\"unit\":\"秒\",\"options\":[]}]}],\"dealer\":{\"componentMeta\":{\"name\":\"示例交易策略\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.dealer.SampleDealer\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"开仓手数\",\"name\":\"openVol\",\"order\":20,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"\",\"options\":[]},{\"label\":\"价格类型\",\"name\":\"priceTypeStr\",\"order\":30,\"type\":\"Options\",\"value\":\"市价\",\"unit\":\"\",\"options\":[\"对手价\",\"市价\",\"最新价\",\"排队价\",\"信号价\"]},{\"label\":\"超价\",\"name\":\"overprice\",\"order\":40,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"Tick\",\"options\":[]}]},\"enabled\":false,\"type\":\"CTA\"}";
		HttpEntity<String> entity = new HttpEntity<>(
				demoStr,
				HttpHeaders.readOnlyHttpHeaders(header));
		ResponseEntity<ResultBean> result = rest.exchange("/module", HttpMethod.POST, entity, ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldSuccessfullyUpdate() {
		shouldSuccessfullyCreate();
		String demoStr = "{\"moduleName\":\"TEST\",\"accountGatewayId\":\"TG1\",\"signalPolicy\":{\"componentMeta\":{\"name\":\"示例策略\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.signal.SampleSignalPolicy\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"短周期\",\"name\":\"shortPeriod\",\"order\":20,\"type\":\"Number\",\"value\":\"20\",\"unit\":\"天\",\"options\":[]},{\"label\":\"长周期\",\"name\":\"longPeriod\",\"order\":30,\"type\":\"Number\",\"value\":\"3\",\"unit\":\"天\",\"options\":[]}]},\"riskControlRules\":[{\"componentMeta\":{\"name\":\"委托超时限制\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.risk.TimeExceededRule\"},\"initParams\":[{\"label\":\"超时时间\",\"name\":\"timeoutSeconds\",\"order\":0,\"type\":\"Number\",\"value\":\"23\",\"unit\":\"秒\",\"options\":[]}]}],\"dealer\":{\"componentMeta\":{\"name\":\"示例交易策略\",\"className\":\"tech.xuanwu.northstar.strategy.cta.module.dealer.SampleDealer\"},\"initParams\":[{\"label\":\"绑定合约\",\"name\":\"bindedUnifiedSymbol\",\"order\":10,\"type\":\"String\",\"value\":\""+symbol+"@SHFE@FUTURES\",\"unit\":\"\",\"options\":[]},{\"label\":\"开仓手数\",\"name\":\"openVol\",\"order\":20,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"\",\"options\":[]},{\"label\":\"价格类型\",\"name\":\"priceTypeStr\",\"order\":30,\"type\":\"Options\",\"value\":\"市价\",\"unit\":\"\",\"options\":[\"对手价\",\"市价\",\"最新价\",\"排队价\",\"信号价\"]},{\"label\":\"超价\",\"name\":\"overprice\",\"order\":40,\"type\":\"Number\",\"value\":\"2\",\"unit\":\"Tick\",\"options\":[]}]},\"enabled\":true,\"type\":\"CTA\"}";
		HttpEntity<String> entity = new HttpEntity<>(
				demoStr,
				HttpHeaders.readOnlyHttpHeaders(header));
		ResponseEntity<ResultBean> result = rest.exchange("/module", HttpMethod.PUT, entity, ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldFindModules() {
		HttpEntity entity = new HttpEntity<>(
				HttpHeaders.readOnlyHttpHeaders(header));
		ResponseEntity<ResultBean> result = rest.exchange("/module", HttpMethod.GET, entity, ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldSuccessfullyRemove() {
		shouldSuccessfullyCreate();
		HttpEntity entity = new HttpEntity<>(
				HttpHeaders.readOnlyHttpHeaders(header));
		ResponseEntity<ResultBean> result = rest.exchange("/module?name=TEST", HttpMethod.DELETE, entity, ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldCreateModulePosition() throws InterruptedException {
		prepareGateway();
		System.out.println("等待gateway初始化");
		Thread.sleep(2000);
		shouldSuccessfullyCreate();
		ModulePosition position = ModulePosition.builder()
				.unifiedSymbol(symbol+"@SHFE@FUTURES")
				.multiplier(10)
				.openTime(System.currentTimeMillis())
				.openPrice(1234)
				.openTradingDay("20210609")
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		HttpEntity entity = new HttpEntity<>(
				position,
				HttpHeaders.readOnlyHttpHeaders(header));
		ResponseEntity<ResultBean> result = rest.exchange("/module/TEST/position", HttpMethod.POST, entity, ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldUpdateModulePosition() throws InterruptedException {
		prepareGateway();
		shouldSuccessfullyCreate();
		ModulePosition position = ModulePosition.builder()
				.unifiedSymbol(symbol+"@SHFE@FUTURES")
				.multiplier(10)
				.openTime(System.currentTimeMillis())
				.openPrice(1234)
				.openTradingDay("20210609")
				.positionDir(PositionDirectionEnum.PD_Long)
				.build();
		HttpEntity entity = new HttpEntity<>(
				position,
				HttpHeaders.readOnlyHttpHeaders(header));
		ResponseEntity<ResultBean> result = rest.exchange("/module/TEST/position", HttpMethod.PUT, entity, ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldRemoveModulePosition() throws InterruptedException {
		shouldCreateModulePosition();
		HttpEntity entity = new HttpEntity<>(
				HttpHeaders.readOnlyHttpHeaders(header));
		ResponseEntity<ResultBean> result = rest.exchange("/module/TEST/position?unifiedSymbol="+symbol+"@SHFE@FUTURES&dir=PD_Long", HttpMethod.DELETE, entity, ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	private void prepareGateway() throws InterruptedException {
		HttpEntity<GatewayDescription> entity2 = new HttpEntity<GatewayDescription>(
				TestGatewayFactory.makeMktGateway("TG5", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class),false),
				HttpHeaders.readOnlyHttpHeaders(header));
		rest.postForObject("/mgt/gateway", entity2, ResultBean.class);
		HttpEntity<GatewayDescription> entity3 = new HttpEntity<GatewayDescription>(
				TestGatewayFactory.makeTrdGateway("TG5Account", "TG5", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class),true),
				HttpHeaders.readOnlyHttpHeaders(header));
		rest.postForObject("/mgt/gateway", entity3, ResultBean.class);
		
		Thread.sleep(1000);
	}
	
//	@Test
//	public void shouldSuccessfullyGetModuleCurrentPerformance() {
//		
//	}
//	
//	@Test
//	public void shouldSuccessfullyGetModuleHistoryPerformance() {}
//	
//	@Test
//	public void shouldSuccessfullyToggleModuleState() {}
//	
	
}
