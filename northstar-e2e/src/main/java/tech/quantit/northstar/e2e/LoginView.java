package tech.quantit.northstar.e2e;

import java.net.SocketException;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import tech.quantit.northstar.main.utils.InetAddressUtils;

public class LoginView {
	
	private WebDriver webDriver;
	
	private URLChangeListener urlListener;
	
	private ErrorHintListener errListener;
	
	private WebElement inputUsername;
	
	private WebElement inputPassword;
	
	private WebElement btnSubmit;
	
	public LoginView(WebDriver webDriver, URLChangeListener urlListener, ErrorHintListener errListener) {
		try {
			webDriver.get("http://" + InetAddressUtils.getInet4Address());
		} catch (SocketException e) {
			throw new IllegalStateException(e);
		}
		this.webDriver = webDriver;
		this.urlListener = urlListener;
		this.errListener = errListener;
		List<WebElement> inputBoxes = webDriver.findElements(By.tagName("input"));
		this.inputUsername = inputBoxes.get(0);
		this.inputPassword = inputBoxes.get(1);
		this.btnSubmit = webDriver.findElement(By.tagName("button"));
	}

	public String getTitle() {
		return webDriver.getTitle();
	}
	
	private void setUsername(String username) {
		inputUsername.sendKeys(username);
	}
	
	private void setPassword(String password) {
		inputPassword.sendKeys(password);
	}
	
	private void submit() {
		String urlBefore = webDriver.getCurrentUrl();
		btnSubmit.click();
		
		try {
			WebElement errHint = webDriver.findElement(By.className("el-message--error"));
			errListener.onError(errHint.getText()); 
		} catch (NoSuchElementException e) {
			// 无须处理
		}
		
		String urlAfter = webDriver.getCurrentUrl();
		if(!urlBefore.equals(urlAfter)) {
			urlListener.onChange(urlAfter);
		}
	}
	
	public void login(String username, String password) {
		setUsername(username);
		setPassword(password);
		submit();
	}
	
}
