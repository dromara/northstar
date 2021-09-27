package integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.alibaba.fastjson.JSON;

import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.model.NsUser;
import tech.xuanwu.northstar.main.NorthstarApplication;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
public class DataSyncBIT {

	@Autowired
	private MockMvc mockMvc;
	
	private MockHttpSession session;
	
	@Before
	public void setUp() throws Exception {
		session = new MockHttpSession();
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON_UTF8).content(JSON.toJSONString(new NsUser("admin","123456"))).session(session))
			.andExpect(status().isOk());
	}
	
	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void shouldSync() throws Exception {
		mockMvc.perform(get("/data/sync").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}


	@Test
	public void shouldGetHistoryBar() throws Exception {
		mockMvc.perform(get("/data/his/bar?gatewayId=test&unifiedSymbol=rb2201@SHFE@FUTURES&startDate=20210808&endDate=20210810").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	@Test
	public void shouldGetAvailableContract() throws Exception {
		mockMvc.perform(get("/data/contracts").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(ReturnCode.SUCCESS));
	}
	
	
}
