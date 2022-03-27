package tech.quantit.northstar.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.SocketException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import tech.quantit.northstar.main.NorthstarApplication;
import tech.quantit.northstar.main.utils.InetAddressUtils;

@SpringBootTest(classes = NorthstarApplication.class, webEnvironment=WebEnvironment.DEFINED_PORT)
public class LoginTest {
	
	static WebDriver driver = ChromeDriverFactory.newSession(1);
	
	static String hostname;

	@BeforeAll
	static void prepare() throws SocketException {
		hostname = "http://" + InetAddressUtils.getInet4Address();
	}
	
	@AfterAll
	static void cleanup() {
		driver.quit();
	}
	
	@BeforeEach
	void openBrowser() {
		driver.get(hostname);
	}
	
	/* 正常用例：可以正常打开网页监控端 */
	@Order(0)
	@Test
	void shouldOpenTheMonitor() {
		assertThat(driver.getTitle()).isEqualTo("Northstar");
	}
	
	/* 正常用例：输入默认的账户密码后，自动跳转到工作区 */
	@Order(30)
	@Test
	void shouldLoginWithCorrectNameAndPassword() {
		List<WebElement> inputBoxes = driver.findElements(By.tagName("input"));
		inputBoxes.get(0).sendKeys("admin");
		inputBoxes.get(1).sendKeys("123456");
		
		WebElement btnConfirm = driver.findElement(By.tagName("button"));
		btnConfirm.click();
		
		assertThrows(NoSuchElementException.class, () -> driver.findElement(By.className("el-message--error")));
		assertThat(driver.getCurrentUrl()).isEqualTo(hostname + "/#/workspace");
	}
	
	/* 异常用例：输入错误的用户名或密码后，弹出错误提示 */
	@Order(10)
	@Test
	void getErrorPromptIfWrongAccountOrPassword() {
		assertThrows(NoSuchElementException.class, () -> driver.findElement(By.className("el-message--error")));

		/* 情况1：什么都不输入 */
		WebElement btnConfirm = driver.findElement(By.tagName("button"));
		btnConfirm.click();
		assertErrorPrompt();
		
		/* 情况2：输入错误用户名 */
		List<WebElement> inputBoxes = driver.findElements(By.tagName("input"));
		inputBoxes.get(0).sendKeys("admin");
		inputBoxes.get(1).sendKeys("654321");
		assertErrorPrompt();
		
		/* 情况3：输入错误密码 */
		inputBoxes.get(0).sendKeys("admins");
		inputBoxes.get(1).sendKeys("123456");
		assertErrorPrompt();
	}
	
	private void assertErrorPrompt() {
		WebElement errorHint = driver.findElement(By.className("el-message--error"));
		assertThat(errorHint).isNotNull();
		
		assertThat(driver.getCurrentUrl()).isEqualTo(hostname + "/#/login");
	}
	
}
