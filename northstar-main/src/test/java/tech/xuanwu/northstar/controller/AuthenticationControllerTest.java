package tech.xuanwu.northstar.controller;

import static org.hamcrest.CoreMatchers.any;
import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpSession;

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

import tech.xuanwu.northstar.common.model.NsUser;

@RunWith(SpringRunner.class)
@WebAppConfiguration
public class AuthenticationControllerTest {
	
	private MockMvc mockMvc;
	
	@Before
	public void init() {
		AuthenticationController ctl = new AuthenticationController();
		ctl.userId = "admin";
		ctl.password = "123456";
		ctl.session = mock(HttpSession.class);
		mockMvc = MockMvcBuilders.standaloneSetup(ctl).build();
	}

	@Test
	public void testDoAuth() throws Exception{
		mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.content(JSON.toJSONString(new NsUser("admin","123456"))))
		.andExpect(MockMvcResultMatchers.status().isOk())
		.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("200"))
		.andExpect(MockMvcResultMatchers.jsonPath("$.data").value(any(String.class)))
		.andDo(MockMvcResultHandlers.print());
		
	}

}
