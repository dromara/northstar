package tech.quantit.northstar.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import tech.quantit.northstar.main.NorthstarApplication;

@SpringBootTest(classes = NorthstarApplication.class, webEnvironment=WebEnvironment.DEFINED_PORT)
public class LoginViewTest {
	
	static WebDriver driver = ChromeDriverFactory.newSession(1);
	
	private LoginView view;
	
	private URLChangeListener urlListener;
	
	private ErrorHintListener errListener;

	@BeforeEach
	public void setup() {
		urlListener = mock(URLChangeListener.class);
		errListener = mock(ErrorHintListener.class);
		view = new LoginView(driver, urlListener, errListener);
	}
	
	@AfterAll
	static void cleanup() {
		driver.quit();
	}
	
	/* 正常用例：可以正常打开网页监控端 */
	@Order(0)
	@Test
	void shouldOpenTheMonitor() {
		assertThat(view.getTitle()).isEqualTo("Northstar");
	}
	
	/* 正常用例：输入默认的账户密码后，自动跳转到工作区 */
	@Order(30)
	@Test
	void shouldLoginWithCorrectNameAndPassword() {
		view.setUsername("admin");
		view.setPassword("123456");
		view.submit();
		
		verify(urlListener, times(1)).onChange(anyString());
		verify(errListener, times(0)).onError(anyString());
	}
	
	/* 异常用例：输入错误的用户名或密码后，弹出错误提示 */
	@Order(10)
	@Test
	void getErrorPromptIfWrongAccountOrPassword() {
		/* 情况1：什么都不输入 */
		view.submit();
		verify(errListener, times(1)).onError(anyString());
		
		/* 情况2：输入错误用户名 */
		view.setUsername("admins");
		view.setPassword("123456");
		view.submit();
		verify(errListener, times(2)).onError(anyString());
		
		/* 情况3：输入错误密码 */
		view.setUsername("admin");
		view.setPassword("987654");
		view.submit();
		verify(errListener, times(3)).onError(anyString());
	}
	
}
