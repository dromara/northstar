package tech.xuanwu.northstar.restful;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.alibaba.fastjson.JSON;

import tech.xuanwu.northstar.common.constant.GatewayType;
import tech.xuanwu.northstar.common.constant.GatewayUsage;
import tech.xuanwu.northstar.common.model.GatewayDescription;
import tech.xuanwu.northstar.restful.GatewayManagementController;
import tech.xuanwu.northstar.service.GatewayService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
public class GatewayManagementControllerTest {

	private MockMvc mockMvc;

	@Before
	public void init() throws Exception {
		GatewayManagementController ctl = new GatewayManagementController();
		ctl.gatewayService = mock(GatewayService.class);
		when(ctl.gatewayService.createGateway(any(GatewayDescription.class))).thenReturn(Boolean.TRUE);
		when(ctl.gatewayService.deleteGateway(anyString())).thenReturn(Boolean.TRUE);
		when(ctl.gatewayService.updateGateway(any(GatewayDescription.class))).thenReturn(Boolean.TRUE);
		mockMvc = MockMvcBuilders.standaloneSetup(ctl).build();
	}

	@Test
	public void testCreate() throws Exception {
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("testGateway")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
		
		mockMvc.perform(MockMvcRequestBuilders.post("/mgt/gateway")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(gd)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("200"))
				.andDo(MockMvcResultHandlers.print());
	}

	@Test
	public void testRemove() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/mgt/gateway")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.param("gatewayId", "testGateway"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("200"))
				.andDo(MockMvcResultHandlers.print());
	}

	@Test
	public void testModify() throws Exception {
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("testGateway")
				.gatewayAdapterType("tech.xuanwu.northstar.gateway.ctp.x64v6v3v15v.CtpGatewayAdapter")
				.gatewayType(GatewayType.CTP)
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
		
		mockMvc.perform(MockMvcRequestBuilders.put("/mgt/gateway")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(gd)))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("200"))
				.andDo(MockMvcResultHandlers.print());
	}

	@Test
	public void testList() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/mgt/gateway").accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("200"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data").isArray())
				.andDo(MockMvcResultHandlers.print());
	}

}
