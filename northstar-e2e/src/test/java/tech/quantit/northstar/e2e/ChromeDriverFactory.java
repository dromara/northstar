package tech.quantit.northstar.e2e;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class ChromeDriverFactory {
	
	static {
		// 需要从此地址(https://chromedriver.storage.googleapis.com/index.html) 下载浏览器驱动，然后把驱动放于以下位置
		System.setProperty("webdriver.chrome.driver", "C:\\Program Files\\Google\\Chrome\\chromedriver.exe");
	}
	
	public static WebDriver newSession(int timeoutSeconds) {
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(timeoutSeconds, TimeUnit.SECONDS);
		return driver;
	}
}
