package tech.xuanwu.northstar.integrated;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.alibaba.fastjson.JSON;

import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.model.CtpSettings;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.persistence.GatewayRepository;
import tech.xuanwu.northstar.persistence.po.GatewayPO;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@TestPropertySource("classpath:application-unittest.properties")
public class GatewayManagementTest {

	MockMvc mockMvc;
	
	@Autowired
	WebApplicationContext ctx;
	
	GatewayDescription mktGateway;
	GatewayDescription trdGateway;
	CtpSettings settings;
	
	@Autowired
	GatewayRepository gwRepo;
	
	@Before
	public void prepare() {
		gwRepo.deleteAll();
		mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
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
				.build();
	}
	
	@Test
	public void testCRUD() throws Exception {
		//新增行情网关
		mockMvc.perform(MockMvcRequestBuilders.post("/mgt/gateway")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(mktGateway)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.returnCode").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
				.andDo(MockMvcResultHandlers.print());
		//新增交易网关
		mockMvc.perform(MockMvcRequestBuilders.post("/mgt/gateway")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(trdGateway)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.returnCode").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
				.andDo(MockMvcResultHandlers.print());
		
		//连接网关
		mockMvc.perform(MockMvcRequestBuilders.get("/mgt/connection")
				.param("gatewayId", "testMarketGateway"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.returnCode").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
				.andDo(MockMvcResultHandlers.print());
		mockMvc.perform(MockMvcRequestBuilders.get("/mgt/connection")
				.param("gatewayId", "testTradeGateway"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.returnCode").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
				.andDo(MockMvcResultHandlers.print());
		
		//等待网关连接
		Thread.sleep(10000);
		
		mockMvc.perform(MockMvcRequestBuilders.delete("/mgt/connection")
				.param("gatewayId", "testMarketGateway"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.returnCode").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
				.andDo(MockMvcResultHandlers.print());
		mockMvc.perform(MockMvcRequestBuilders.delete("/mgt/connection")
				.param("gatewayId", "testTradeGateway"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.returnCode").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
				.andDo(MockMvcResultHandlers.print());
		
		//等待网关断开
		Thread.sleep(10000);
		
		//查询全部
		mockMvc.perform(MockMvcRequestBuilders.get("/mgt/gateway")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(2))
				.andDo(MockMvcResultHandlers.print());
		//查询行情网关
		mockMvc.perform(MockMvcRequestBuilders.get("/mgt/gateway")
				.param("usage", GatewayUsage.MARKET_DATA.toString())
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(1))
				.andDo(MockMvcResultHandlers.print());
		//查询交易网关
		mockMvc.perform(MockMvcRequestBuilders.get("/mgt/gateway")
				.param("usage", GatewayUsage.TRADE.toString())
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.length()").value(1))
				.andDo(MockMvcResultHandlers.print());
		
		mktGateway = GatewayDescription.builder()
				.gatewayId("testMarketGateway")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.description("testingUpdate")
				.settings(settings)
				.build();
		mockMvc.perform(MockMvcRequestBuilders.put("/mgt/gateway")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(mktGateway)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
				.andDo(MockMvcResultHandlers.print());
		
		Optional<GatewayPO> result = gwRepo.findById("testMarketGateway");
		assertThat(result.get().getDescription()).isEqualTo("testingUpdate");
		
		//删除行情网关
		mockMvc.perform(MockMvcRequestBuilders.delete("/mgt/gateway")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.param("gatewayId", "testMarketGateway"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
				.andDo(MockMvcResultHandlers.print());
		//删除交易网关
		mockMvc.perform(MockMvcRequestBuilders.delete("/mgt/gateway")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.param("gatewayId", "testTradeGateway"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(true))
				.andDo(MockMvcResultHandlers.print());
		
		assertThat(gwRepo.count()).isEqualTo(0);
	}
	
}
