package tech.quantit.northstar.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import tech.quantit.northstar.main.NorthstarApplication;

@SpringBootTest(classes = NorthstarApplication.class, webEnvironment=WebEnvironment.DEFINED_PORT, value="spring.profiles.active=e2e")
public class MarketGatewayMgmtTest {

	static WebDriver driver = ChromeDriverFactory.newSession(1);
	
	private static LoginView loginView;
	
	private static ErrorHintListener errListener;
	
	private static MarketGatewayMgmtView gatewayMgmtView;
	
	@BeforeEach
	public void setup() {
		loginView = new LoginView(driver, mock(URLChangeListener.class), mock(ErrorHintListener.class));
		loginView.login("admin", "123456");
		errListener = mock(ErrorHintListener.class);
		gatewayMgmtView = new MarketGatewayMgmtView(driver, errListener);
	}
	
	@BeforeAll
	static void prepare() {
		TestMongoUtils.clearDB();
	}
	
	@AfterAll
	static void endup() {
		TestMongoUtils.clearDB();
		driver.close();
	}
	
	/* 正常用例：创建CTP行情网关成功 */
	@Test
	void shouldCreateCTPGateway() throws InterruptedException {
		List<WebElement> rowsBefore = driver.findElements(By.className("el-table__row"));
		assertThat(rowsBefore).isEmpty();
		gatewayMgmtView.createCtpGateway("ctp", "123456", "main");
		List<WebElement> rowsAfter = driver.findElements(By.className("el-table__row"));
		assertThat(rowsAfter).hasSize(1);
		verify(errListener, times(0)).onError(anyString());
		
		gatewayMgmtView.removeCtpGateway();
	}
	
	/* 正常用例：创建SIM行情网关成功 */
	@Test
	void shouldCreateSIMGateway() {
		List<WebElement> rowsBefore = driver.findElements(By.className("el-table__row"));
		assertThat(rowsBefore).isEmpty();
		gatewayMgmtView.createSimGateway();
		List<WebElement> rowsAfter = driver.findElements(By.className("el-table__row"));
		assertThat(rowsAfter).hasSize(1);
		verify(errListener, times(0)).onError(anyString());
		
		gatewayMgmtView.removeSimGateway();
	}
	
	/* 异常用例：重复创建CTP行情网关失败 */
	@Test
	void shouldFailIfCreateCTPGatewayMoreThanOnce() {
		gatewayMgmtView.createCtpGateway("ctp", "123456", "main");
		verify(errListener, times(0)).onError(anyString());
		gatewayMgmtView.createCtpGateway("ctp", "123456", "main");
		verify(errListener, times(1)).onError(anyString());
		
		gatewayMgmtView.removeCtpGateway();
	}
	
	/* 异常用例：重复创建SIM行情网关失败 */
	@Test
	void shouldFailIfCreateSIMGatewayMoreThanOnce() {
		gatewayMgmtView.createSimGateway();
		verify(errListener, times(0)).onError(anyString());
		gatewayMgmtView.createSimGateway();
		verify(errListener, times(1)).onError(anyString());
		
		gatewayMgmtView.removeSimGateway();
	}
	
	/* 正常用例：删除CTP行情网关成功 */
	@Test
	void shouldRemoveCTPGateway() {
		gatewayMgmtView.createCtpGateway("ctp", "123456", "main");
		List<WebElement> rowsBefore = driver.findElements(By.className("el-table__row"));
		assertThat(rowsBefore).isNotEmpty();
		
		gatewayMgmtView.removeCtpGateway();
		List<WebElement> rowsAfter = driver.findElements(By.className("el-table__row"));
		assertThat(rowsAfter).isEmpty();
		verify(errListener, times(0)).onError(anyString());
	}
	
	/* 正常用例：删除SIM行情网关成功 */
	@Test
	void shouldRemoveSIMGateway() {
		gatewayMgmtView.createSimGateway();
		List<WebElement> rowsBefore = driver.findElements(By.className("el-table__row"));
		assertThat(rowsBefore).isNotEmpty();
		
		gatewayMgmtView.removeSimGateway();
		List<WebElement> rowsAfter = driver.findElements(By.className("el-table__row"));
		assertThat(rowsAfter).isEmpty();
		verify(errListener, times(0)).onError(anyString());
	}
	
	/* 异常用例：仍有账户网关绑定时，删除行情网关失败 */
	
}
