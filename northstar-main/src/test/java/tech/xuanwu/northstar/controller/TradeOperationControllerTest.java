package tech.xuanwu.northstar.controller;

import static org.hamcrest.CoreMatchers.any;

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
public class TradeOperationControllerTest {

	private MockMvc mockMvc;
	
	@Before
	public void init() {
		mockMvc = MockMvcBuilders.standaloneSetup(new TradeOperationController()).build();
	}
	
	@Test
	public void testSubmitOrder() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/trade/submit").accept(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.content().string(any(String.class)))
			.andDo(MockMvcResultHandlers.print());
	}

	@Test
	public void testCancelOrder() throws Exception{
		mockMvc.perform(MockMvcRequestBuilders.post("/trade/cancel").accept(MediaType.APPLICATION_JSON))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.content().string("true"))
			.andDo(MockMvcResultHandlers.print());
	}

}
