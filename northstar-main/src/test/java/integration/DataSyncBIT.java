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
import org.springframework.web.client.RestTemplate;

import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.model.NsUser;
import tech.xuanwu.northstar.common.model.ResultBean;

public class DataSyncBIT {

private String cookie;
	
	private RestTemplate rest = new RestTemplateBuilder().rootUri("http://localhost:8888/northstar").build();
	
	private HttpHeaders header;
	
	@Before
	public void setUp() {
		ResponseEntity<ResultBean> result = rest.postForEntity("/auth/login", new NsUser("admin","123456"), ResultBean.class);
		cookie = result.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
		header = new HttpHeaders();
		header.add("Cookie", cookie);
	}
	
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldSync() {
		ResponseEntity<ResultBean> result = rest.exchange("/data/sync", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}

	@Test
	public void shouldGetHistoryBar() {
		ResponseEntity<ResultBean> result = rest.exchange("/data/his/bar?gatewayId=test&unifiedSymbol=rb2201@SHFE@FUTURES&startDate=20210808&endDate=20210810", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void shouldGetAvailableContract() {
		ResponseEntity<ResultBean> result = rest.exchange("/data/contracts", HttpMethod.GET, new HttpEntity(HttpHeaders.readOnlyHttpHeaders(header)), ResultBean.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	
}
