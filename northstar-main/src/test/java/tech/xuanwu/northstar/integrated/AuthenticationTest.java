package tech.xuanwu.northstar.integrated;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import tech.xuanwu.northstar.common.constant.ReturnCode;
import tech.xuanwu.northstar.common.model.NsUser;
import tech.xuanwu.northstar.common.model.ResultBean;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-unittest.properties")
public class AuthenticationTest {

	@Autowired
	private TestRestTemplate restTemplate;
	
	@Test
	public void test_NS34_CorrectUserIdAndPassword() {
		ResultBean<String> result = restTemplate.postForObject("/auth/login", new NsUser("admin","123456"), ResultBean.class);
		assertThat(result.getStatus()).isEqualTo(ReturnCode.SUCCESS);
		assertThat(StringUtils.hasText(result.getData())).isTrue();
	}
	
	@Test
	public void test_NS36_WrongUserIdAndPassword() {
		ResultBean<String> result1 = restTemplate.postForObject("/auth/login", new NsUser("KEVIN","123456"), ResultBean.class);
		assertThat(result1.getStatus()).isEqualTo(ReturnCode.AUTH_ERR);
		assertThat(StringUtils.hasText(result1.getMessage())).isTrue();
		
		ResultBean<String> result2 = restTemplate.postForObject("/auth/login", new NsUser("admin","00000"), ResultBean.class);
		assertThat(result2.getStatus()).isEqualTo(ReturnCode.AUTH_ERR);
		assertThat(StringUtils.hasText(result2.getMessage())).isTrue();
	}
	
}
