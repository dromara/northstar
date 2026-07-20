package org.dromara.northstar.app.restful;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dromara.northstar.NorthstarApplication;
import org.dromara.northstar.app.service.ModuleService;
import org.dromara.northstar.common.model.ModuleMockTradeDTO;
import org.dromara.northstar.strategy.IMessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.alibaba.fastjson2.JSON;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=unittest")
@AutoConfigureMockMvc
class ModuleControllerTest {

	@Autowired
	MockMvc mvc;
	
	@MockBean
	ModuleService service;
	
	@MockBean
	private IMessageSender sender;
	
	String testModule = "{\"name\":\"TEST\",\"type\":\"SPECULATION\",\"usage\":\"PLAYBACK\",\"weeksOfDataForPreparation\":0,\"moduleCacheDataSize\":500,\"closingPolicy\":\"FIRST_IN_FIRST_OUT\",\"initBalance\":10000,\"defaultVolume\":1,\"orderPlusTick\":0,\"bindedAccounts\":[\"模拟账户\"],\"bindedContracts\":[{\"id\":\"ao0000@SHFE@FUTURES@PLAYBACK\",\"name\":\"氧化铝指数\",\"symbol\":\"ao0000\",\"unifiedSymbol\":\"ao0000@SHFE@FUTURES\",\"channelType\":\"PLAYBACK\",\"precision\":0,\"dueDate\":\"+9999999991231\"}],\"strategySetting\":{\"name\":\"示例-简单策略\",\"className\":\"org.dromara.northstar.strategy.example.BeginnerExampleStrategy\",\"settings\":[{\"label\":\"操作间隔\",\"name\":\"actionInterval\",\"order\":10,\"type\":\"NUMBER\",\"value\":\"60\",\"unit\":\"秒\",\"options\":[],\"optionsVal\":[],\"placeholder\":\"\",\"required\":true},{\"label\":\"锁仓演示\",\"name\":\"showHedge\",\"order\":20,\"type\":\"SELECT\",\"value\":\"false\",\"unit\":\"\",\"options\":[\"true\",\"false\"],\"optionsVal\":[],\"placeholder\":\"\",\"required\":true},{\"label\":\"反手演示\",\"name\":\"showReverse\",\"order\":21,\"type\":\"SELECT\",\"value\":\"false\",\"unit\":\"\",\"options\":[\"true\",\"false\"],\"optionsVal\":[],\"placeholder\":\"\",\"required\":true},{\"label\":\"价格类型\",\"name\":\"priceType\",\"order\":30,\"type\":\"SELECT\",\"value\":\"OPP_PRICE\",\"unit\":\"\",\"options\":[\"市价\",\"对手价\",\"排队价\"],\"optionsVal\":[\"ANY_PRICE\",\"OPP_PRICE\",\"WAITING_PRICE\"],\"placeholder\":\"\",\"required\":true}]}}";
	
	@Test
	void testCreateModule() throws Exception {
		mvc.perform(MockMvcRequestBuilders.post("/northstar/module")
				.contentType(MediaType.APPLICATION_JSON)
				.content(testModule))
			.andExpect(status().isOk());
	}

	@Test
	void testUpdateModule() throws Exception {
		mvc.perform(MockMvcRequestBuilders.put("/northstar/module")
				.param("reset", "false")
				.contentType(MediaType.APPLICATION_JSON)
				.content(testModule))
			.andExpect(status().isOk());
		
		mvc.perform(MockMvcRequestBuilders.put("/northstar/module")
				.param("reset", "true")
				.contentType(MediaType.APPLICATION_JSON)
				.content(testModule))
			.andExpect(status().isOk());
	}

	@Test
	void testGetModules() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/module"))
			.andExpect(status().isOk());
	}

	@Test
	void testRemoveModule() throws Exception {
		mvc.perform(MockMvcRequestBuilders.delete("/northstar/module")
				.param("moduleName", "testModule"))
			.andExpect(status().isOk());
	}

	@Test
	void testEnableModule() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/module/testModule/enable"))
			.andExpect(status().isOk());
	}

	@Test
	void testDisableModule() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/module/testModule/disable"))
			.andExpect(status().isOk());
	}

	@Test
	void testGetRuntime() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/module/testModule/rt"))
			.andExpect(status().isOk());
	}

	@Test
	void testGetDeals() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/module/testModule/deals"))
			.andExpect(status().isOk());
	}

	@Test
	void testGetChart() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/module/testModule/chart"))
			.andExpect(status().isOk());
	}

	@Test
	void testBacktest() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/northstar/module/testModule/backtest")
				.param("numOfMonthToTest", "3"))
			.andExpect(status().isOk());
	}

	@Test
	void testMockTrade() throws Exception {
		ModuleMockTradeDTO tradeDTO = ModuleMockTradeDTO.builder().build();
		mvc.perform(MockMvcRequestBuilders.post("/northstar/module/mocktrade")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.toJSONString(tradeDTO)))
			.andExpect(status().isOk());
	}
}
