/**
 * 
 */
package integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import common.TestGatewayFactory;
import common.TestMongoUtils;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.NsUser;
import tech.xuanwu.northstar.common.model.ResultBean;
import tech.xuanwu.northstar.common.model.SimSettings;

/**
 * @author kevin
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
		assertThat(result.getData()).isTrue();
		
		entity = new HttpEntity<GatewayDescription>(
				TestGatewayFactory.makeTrdGateway("TG2", "TG1", GatewayType.SIM, TestGatewayFactory.makeGatewaySettings(SimSettings.class),false),
				HttpHeaders.readOnlyHttpHeaders(header));
		result = rest.postForObject("/mgt/gateway", entity, ResultBean.class);
		assertThat(result.getData()).isTrue();
	}
	
	@Test
	public void shouldFindCreatedGateway() {
		shouldCreateGateway();
		
		ResponseEntity<ResultBean> result1 = rest.exchange("/mgt/gateway?usage={1}", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class, GatewayUsage.MARKET_DATA);
		List<LinkedHashMap<String, Object>> list1 = (List<LinkedHashMap<String, Object>>) result1.getBody().getData();
		assertThat(list1.size()).isEqualTo(1);
		assertThat(list1.get(0).get("gatewayId")).isEqualTo("TG1");
		
		ResponseEntity<ResultBean> result2 = rest.exchange("/mgt/gateway?usage={1}", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class, GatewayUsage.TRADE);
		List<LinkedHashMap<String, Object>> list2 = (List<LinkedHashMap<String, Object>>) result2.getBody().getData();
		assertThat(list2.size()).isEqualTo(1);
		assertThat(list2.get(0).get("gatewayId")).isEqualTo("TG2");
	}

	@Test
	public void shouldUpdateGateway() {
		shouldCreateGateway();
		
		HttpEntity entity = new HttpEntity<GatewayDescription>(
				TestGatewayFactory.makeTrdGateway("TG2", "TG1", GatewayType.CTP, TestGatewayFactory.makeGatewaySettings(CtpSettings.class),false),
				HttpHeaders.readOnlyHttpHeaders(header));
		rest.put("/mgt/gateway", entity);
		
		ResponseEntity<ResultBean> result2 = rest.exchange("/mgt/gateway?usage={1}", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class, GatewayUsage.TRADE);
		List<LinkedHashMap<String, Object>> list2 = (List<LinkedHashMap<String, Object>>) result2.getBody().getData();
		assertThat(list2.size()).isEqualTo(1);
		assertThat(list2.get(0).get("gatewayId")).isEqualTo("TG2");
		assertThat(list2.get(0).get("gatewayType")).isEqualTo("CTP");
	}
	
	
	@Test
	public void shouldRemoveGateway() {
		shouldCreateGateway();
		
	}
	
	@Test
	public void shouldFailIfNotProvidingSetting() {}
	
	@Test
	public void shouldFailIfNoInfoProvided() {}
	
	@Test
	public void shouldSuccessWhenGettingState() {}
	
	@Test
	public void shouldSuccessWhenConnecting() {}
	
	@Test
	public void shouldFailIfGatewayNotFound() {}
	
	@Test
	public void shouldSuccessWhenDisconnecting() {}
	
	@Test
	public void shouldIncreaseBalance() {}
	
	@Test
	public void shouldDecreaseBalance() {}
	
	@Test
	public void shouldFailIfNotSimGateway() {}
}
