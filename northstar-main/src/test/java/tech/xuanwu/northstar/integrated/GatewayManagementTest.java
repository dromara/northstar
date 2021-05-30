package tech.xuanwu.northstar.integrated;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.common.constant.ConnectionState;
import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.common.model.NsUser;
import tech.xuanwu.northstar.persistence.GatewayRepository;
import tech.xuanwu.northstar.persistence.po.GatewayPO;
import tech.xuanwu.northstar.restful.common.ResultBean;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-unittest.properties")
public class GatewayManagementTest {

	GatewayDescription mktGateway;
	GatewayDescription trdGateway;
	CtpSettings settings;
	
	@Autowired
	GatewayRepository gwRepo;
	
	@Autowired
	MongoTemplate mongo;
	
	@Autowired
	TestRestTemplate restTemplate;
	
	HttpHeaders headers;
	
	@Before
	public void prepare() {
		gwRepo.deleteAll();
		settings = new CtpSettings();
		settings.setUserId("guest");
		settings.setPassword("123456");
		settings.setBrokerId("9999");
		settings.setMdHost("180.168.146.187");
		settings.setMdPort("10131");
		settings.setTdHost("180.168.146.187");
		settings.setTdPort("10130");
		settings.setAuthCode("0000000000000000");
		settings.setAppId("simnow_client_test");
		settings.setUserProductInfo("simnow_client_test");
		mktGateway = GatewayDescription.builder()
				.gatewayId("testMarketGateway")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.description("testing")
				.settings(settings)
				.build();
		
		trdGateway = GatewayDescription.builder()
				.gatewayId("testTradeGateway")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.TRADE)
				.description("testing")
				.settings(settings)
				.connectionState(ConnectionState.CONNECTED)
				.build();
		
		ResponseEntity result = restTemplate.postForEntity("/auth/login", new NsUser("admin","123456"), ResultBean.class);
		String cookie = result.getHeaders().get("Set-Cookie").get(0);
		headers = new HttpHeaders();
		headers.put(HttpHeaders.COOKIE, List.of(cookie));
	}
	
	@After
	public void clear() {
		for(String name : mongo.getCollectionNames()) {
			mongo.dropCollection(name);
		}
	}
	
	@Test
	public void test_NS33_InvokeWithoutAuth() {
		ResultBean result1 = restTemplate.postForObject("/mgt/gateway", mktGateway, ResultBean.class);
		assertThat(result1.getStatus()).isEqualTo(ReturnCode.AUTH_ERR);
	}
	
	@Test
	public void test_NS37_CreateGateway() {
		ResponseEntity<ResultBean> response = restTemplate.exchange("/mgt/gateway", HttpMethod.POST, new HttpEntity(mktGateway, headers), ResultBean.class);
		ResultBean result = response.getBody();
		assertThat(result.getStatus()).isEqualTo(ReturnCode.SUCCESS);
		Optional<GatewayPO> obj = gwRepo.findById("testMarketGateway");
		assertThat(obj).isNotNull();
		
		ResponseEntity<ResultBean> resp1 = restTemplate.exchange("/mgt/gateway?gatewayId=testMarketGateway", HttpMethod.DELETE, new HttpEntity(null, headers), ResultBean.class);
		ResultBean result1 = resp1.getBody();
		assertThat(result1.getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void test_NS38_ModifyGateway() {
		restTemplate.exchange("/mgt/gateway", HttpMethod.POST, new HttpEntity(mktGateway, headers), ResultBean.class);
		Optional<GatewayPO> obj1 = gwRepo.findById("testMarketGateway");
		
		mktGateway.setDescription("changeit");
		restTemplate.exchange("/mgt/gateway", HttpMethod.PUT, new HttpEntity(mktGateway, headers), ResultBean.class);
		Optional<GatewayPO> obj2 = gwRepo.findById("testMarketGateway");
		
		assertThat(obj1.get()).isNotEqualTo(obj2.get());
		assertThat(obj1.get().getGatewayId()).isEqualTo(obj2.get().getGatewayId());
		assertThat(gwRepo.count()).isEqualTo(1);
		
		ResponseEntity<ResultBean> resp1 = restTemplate.exchange("/mgt/gateway?gatewayId=testMarketGateway", HttpMethod.DELETE, new HttpEntity(null, headers), ResultBean.class);
		ResultBean result1 = resp1.getBody();
		assertThat(result1.getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void test_NS39_RemoveGateway() {
		restTemplate.exchange("/mgt/gateway", HttpMethod.POST, new HttpEntity(mktGateway, headers), ResultBean.class);
		assertThat(gwRepo.count()).isEqualTo(1);
		restTemplate.exchange("/mgt/gateway?gatewayId=" + mktGateway.getGatewayId(), HttpMethod.DELETE, new HttpEntity(null, headers), ResultBean.class);
		assertThat(gwRepo.count()).isEqualTo(0);
	}
	
	@Test
	public void test_NS40_GetAllGateway() {
		trdGateway.setConnectionState(ConnectionState.DISCONNECTED);
		
		restTemplate.exchange("/mgt/gateway", HttpMethod.POST, new HttpEntity(mktGateway, headers), ResultBean.class);
		restTemplate.exchange("/mgt/gateway", HttpMethod.POST, new HttpEntity(trdGateway, headers), ResultBean.class);
		assertThat(gwRepo.count()).isEqualTo(2);
		
		ResponseEntity<ResultBean> resp = restTemplate.exchange("/mgt/gateway", HttpMethod.GET, new HttpEntity(mktGateway, headers), ResultBean.class);
		ResultBean result = resp.getBody();
		List<GatewayDescription> list = JSON.parseArray(JSON.toJSONString(result.getData()), GatewayDescription.class);
		assertThat(list.size()).isEqualTo(2);
		
		ResponseEntity<ResultBean> resp1 = restTemplate.exchange("/mgt/gateway?usage=MARKET_DATA", HttpMethod.GET, new HttpEntity(null, headers), ResultBean.class);
		ResultBean result1 = resp1.getBody();
		List<GatewayDescription> list1 = JSON.parseArray(JSON.toJSONString(result1.getData()), GatewayDescription.class);
		assertThat(list1.get(0).getGatewayId()).isEqualTo(mktGateway.getGatewayId());
		
		ResponseEntity<ResultBean> resp2 = restTemplate.exchange("/mgt/gateway?usage=TRADE", HttpMethod.GET, new HttpEntity(null, headers), ResultBean.class);
		ResultBean result2 = resp2.getBody();
		List<GatewayDescription> list2 = JSON.parseArray(JSON.toJSONString(result2.getData()), GatewayDescription.class);
		assertThat(list2.get(0).getGatewayId()).isEqualTo(trdGateway.getGatewayId());
		
		ResponseEntity<ResultBean> resp3 = restTemplate.exchange("/mgt/gateway?gatewayId=testMarketGateway", HttpMethod.DELETE, new HttpEntity(null, headers), ResultBean.class);
		ResultBean result3 = resp3.getBody();
		assertThat(result3.getStatus()).isEqualTo(ReturnCode.SUCCESS);
		
		ResponseEntity<ResultBean> resp5 = restTemplate.exchange("/mgt/gateway?gatewayId=testTradeGateway", HttpMethod.DELETE, new HttpEntity(null, headers), ResultBean.class);
		ResultBean result5 = resp5.getBody();
		assertThat(result5.getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void test_NS42_CreateGatewayWithDuplicatedID() {
		ResponseEntity<ResultBean> resp1 = restTemplate.exchange("/mgt/gateway", HttpMethod.POST, new HttpEntity(mktGateway, headers), ResultBean.class);
		ResultBean result1 = resp1.getBody();
		assertThat(result1.getStatus()).isEqualTo(ReturnCode.SUCCESS);
		ResponseEntity<ResultBean> resp2 = restTemplate.exchange("/mgt/gateway", HttpMethod.POST, new HttpEntity(mktGateway, headers), ResultBean.class);
		ResultBean result2 = resp2.getBody();
		assertThat(result2.getStatus()).isEqualTo(ReturnCode.ERROR);
		
		ResponseEntity<ResultBean> resp3 = restTemplate.exchange("/mgt/gateway?gatewayId=testMarketGateway", HttpMethod.DELETE, new HttpEntity(null, headers), ResultBean.class);
		ResultBean result3 = resp3.getBody();
		assertThat(result3.getStatus()).isEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void test_NS43_RemoveGatewayWithNonexistID() {
		ResponseEntity<ResultBean> resp1 = restTemplate.exchange("/mgt/gateway?gatewayId=123", HttpMethod.DELETE, new HttpEntity(null, headers), ResultBean.class);
		ResultBean result1 = resp1.getBody();
		assertThat(result1.getStatus()).isEqualTo(ReturnCode.NO_SUCH_ELEMENT_EXCEPTION);
	}
	
	@Deprecated
	public void test_NS44_DisableGateway() {
		ResponseEntity<ResultBean> response = restTemplate.exchange("/mgt/gateway", HttpMethod.POST, new HttpEntity(mktGateway, headers), ResultBean.class);
		
		ResponseEntity<ResultBean> response1 = restTemplate.exchange("/mgt/connection", HttpMethod.GET, new HttpEntity(null, headers), ResultBean.class);
		assertThat(response1.getBody().getStatus()).isNotEqualTo(ReturnCode.SUCCESS);
		
		ResponseEntity<ResultBean> response2 = restTemplate.exchange("/mgt/connection", HttpMethod.DELETE, new HttpEntity(null, headers), ResultBean.class);
		assertThat(response2.getBody().getStatus()).isNotEqualTo(ReturnCode.SUCCESS);
	}
	
	@Test
	public void test_NS52_RemoveGatewayWithNonDisconnect() {
		trdGateway.setConnectionState(ConnectionState.CONNECTED);
		ResponseEntity<ResultBean> resp1 = restTemplate.exchange("/mgt/gateway", HttpMethod.POST, new HttpEntity(trdGateway, headers), ResultBean.class);
		ResultBean result1 = resp1.getBody();
		assertThat(result1.getStatus()).isEqualTo(ReturnCode.SUCCESS);
		
		ResponseEntity<ResultBean> resp2 = restTemplate.exchange("/mgt/gateway?gatewayId=testTradeGateway", HttpMethod.DELETE, new HttpEntity(null, headers), ResultBean.class);
		ResultBean result2 = resp2.getBody();
		assertThat(result2.getStatus()).isEqualTo(ReturnCode.ERROR);
	}
}
