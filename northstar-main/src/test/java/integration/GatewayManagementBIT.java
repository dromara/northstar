package integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import common.TestGatewayFactory;
import common.TestMongoUtils;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.NsUser;
import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.common.model.SimSettings;

/**
 * GatewayManagement接口黑盒测试类
 * @author KevinHuangwl
 *
 */
public class GatewayManagementBIT {
	
	private String cookie;
	
	private RestTemplate rest = new RestTemplateBuilder().rootUri("http://localhost:8888/northstar").build();
	
	private HttpHeaders header;
	
	@Before
	public void setUp() {
		ResponseEntity<ResultBean> result = rest.postForEntity("/auth/login", new NsUser("admin","123456"), ResultBean.class);
		cookie = result.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
		System.out.println("cookie: " + cookie);
		header = new HttpHeaders();
		header.add("Cookie", cookie);
	}
	
	@After
	public void tearDown() {
		TestMongoUtils.clearDB();
	}
	
	@Test
	public void shouldFailWithoutAuth() {
		try {			
			rest.postForEntity("/mgt/gateway", TestGatewayFactory.makeMktGateway("testGateway", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class), false), ResultBean.class);
		} catch (HttpClientErrorException e) {
			assertThat(e.getRawStatusCode()).isEqualTo(401);
		}
	}

	@Test
	public void shouldCreateGateway() {
		HttpEntity<GatewayDescription> entity = new HttpEntity<GatewayDescription>(
				TestGatewayFactory.makeMktGateway("TG1", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class),false),
				HttpHeaders.readOnlyHttpHeaders(header));
		ResultBean<Boolean> result = rest.postForObject("/mgt/gateway", entity, ResultBean.class);
		assertThat(result.getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldFindCreatedGateway() {
		shouldCreateGateway();
		
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/gateway?usage={1}", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class, GatewayUsage.MARKET_DATA);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}

	@Test
	public void shouldUpdateGateway() {
		shouldCreateGateway();
		
		HttpEntity entity = new HttpEntity<GatewayDescription>(
				TestGatewayFactory.makeMktGateway("TG1", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class),false),
				HttpHeaders.readOnlyHttpHeaders(header));
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/gateway", HttpMethod.PUT, entity, ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	
	@Test
	public void shouldRemoveGateway() {
		shouldCreateGateway();
		
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/gateway?gatewayId=TG1", HttpMethod.DELETE, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldFailIfNotProvidingSetting() {
		HttpEntity<GatewayDescription> entity = new HttpEntity<GatewayDescription>(
				TestGatewayFactory.makeMktGateway("TG1", GatewayType.CTP, null,false),
				HttpHeaders.readOnlyHttpHeaders(header));
		ResultBean<Boolean> result = rest.postForObject("/mgt/gateway", entity, ResultBean.class);
		assertThat(result.getStatus()).isEqualTo(ReturnCode.ERROR);
	}
	
	@Test(expected = HttpClientErrorException.class)
	public void shouldFailIfNoInfoProvided() {
		rest.postForObject("/mgt/gateway", null, ResultBean.class);
	}
	
	@Test
	public void shouldSuccessWhenGettingActiveState() {
		shouldCreateGateway();
		
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/gateway/active?gatewayId=TG1", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldSuccessWhenConnecting() {
		shouldCreateGateway();
		
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/connection?gatewayId=TG1", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldSuccessWhenDisconnecting() {
		shouldCreateGateway();
		
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/connection?gatewayId=TG1", HttpMethod.DELETE, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldFailIfGatewayNotFound() {
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/connection?gatewayId=ANY", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.NO_SUCH_ELEMENT_EXCEPTION);
		ResponseEntity<ResultBean> result2 = rest.exchange("/mgt/connection?gatewayId=ANY", HttpMethod.DELETE, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result2.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result2.getBody().getStatus()).isEqualTo(ReturnCode.NO_SUCH_ELEMENT_EXCEPTION);
	}
	
	@Test
	public void shouldIncreaseBalance() {
		shouldCreateGateway();
		
		HttpEntity<GatewayDescription> entity = new HttpEntity<GatewayDescription>(
				TestGatewayFactory.makeTrdGateway("TG2", "", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class), false),
				HttpHeaders.readOnlyHttpHeaders(header));
		rest.postForObject("/mgt/gateway", entity, ResultBean.class);
		
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/moneyio?gatewayId=TG2&money=10000", HttpMethod.POST, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldDecreaseBalance() {
		shouldIncreaseBalance();
		
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/moneyio?gatewayId=TG2&money=-10000", HttpMethod.POST, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldFailIfNotSimGateway() {
		shouldCreateGateway();
		
		ResponseEntity<ResultBean> result = rest.exchange("/mgt/moneyio?gatewayId=TG1&money=10000", HttpMethod.POST, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.ERROR);
	}
}
