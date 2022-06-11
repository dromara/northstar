package tech.quantit.northstar.main.restful;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.corundumstudio.socketio.SocketIOServer;

import tech.quantit.northstar.main.NorthstarApplication;

@SpringBootTest(classes = NorthstarApplication.class, value="spring.profiles.active=test")
@AutoConfigureMockMvc
class RedirectControllerTest {
	
	@Autowired
	private MockMvc mockMvc;
	
	@MockBean
	private SocketIOServer socketServer;
	
	@Test
	void test() throws Exception {
		String result = mockMvc.perform(get("/redirect"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(result).isNotEmpty();
	}

}
