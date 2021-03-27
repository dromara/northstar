package tech.xuanwu.northstar.controller;

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

@RunWith(SpringRunner.class)
@WebAppConfiguration
public class TradeGatewayManagementControllerTest {

	private MockMvc mockMvc;

	@Before
	public void init() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TradeGatewayManagementController()).build();
	}

	@Test
	public void testCreate() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/trade/gateway").accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("true"))
				.andDo(MockMvcResultHandlers.print());
	}

	@Test
	public void testRemove() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/trade/gateway").accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("true"))
				.andDo(MockMvcResultHandlers.print());
	}

	@Test
	public void testModify() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.put("/trade/gateway").accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.content().string("true"))
				.andDo(MockMvcResultHandlers.print());
	}

	@Test
	public void testList() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/trade/gateway").accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
				.andDo(MockMvcResultHandlers.print());
	}

}
