package tech.xuanwu.northstar.restful;

import static org.mockito.ArgumentMatchers.any;
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

import tech.xuanwu.northstar.common.exception.TradeException;
import tech.xuanwu.northstar.common.model.OrderRecall;
import tech.xuanwu.northstar.common.model.OrderRequest;
import tech.xuanwu.northstar.common.model.OrderRequest.TradeOperation;
import tech.xuanwu.northstar.restful.TradeOperationController;
import tech.xuanwu.northstar.service.AccountService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
public class TradeOperationControllerTest {

	private MockMvc mockMvc;
	
	@Before
	public void init() throws TradeException {
		TradeOperationController ctl = new TradeOperationController();
		ctl.accountService = mock(AccountService.class);
		when(ctl.accountService.submitOrder(any(OrderRequest.class))).thenReturn(Boolean.TRUE);
		when(ctl.accountService.cancelOrder(any(OrderRecall.class))).thenReturn(Boolean.TRUE);
		mockMvc = MockMvcBuilders.standaloneSetup(ctl).build();
	}
	
	@Test
	public void testSubmitOrder() throws Exception {
		OrderRequest req = OrderRequest.builder()
				.gatewayId("testAccount")
				.contractUnifiedSymbol("rb12345")
				.price("123.45")
				.tradeOpr(TradeOperation.BK)
				.volume(1)
				.build();
		
		mockMvc.perform(MockMvcRequestBuilders.post("/trade/submit")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(req)))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("200"))
			.andDo(MockMvcResultHandlers.print());
	}

	@Test
	public void testCancelOrder() throws Exception{
		OrderRecall recall = OrderRecall.builder()
				.orderId("123456789")
				.build();
		
		mockMvc.perform(MockMvcRequestBuilders.post("/trade/cancel")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(recall)))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("200"))
			.andDo(MockMvcResultHandlers.print());
	}

}
