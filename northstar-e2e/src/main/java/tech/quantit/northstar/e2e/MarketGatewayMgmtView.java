package tech.quantit.northstar.e2e;

import java.net.SocketException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import tech.quantit.northstar.main.utils.InetAddressUtils;

public class MarketGatewayMgmtView {

	private ErrorHintListener errListener;
	
	private WebDriver webDriver;
	
	public MarketGatewayMgmtView(WebDriver webDriver, ErrorHintListener errListener) {
		try {
			webDriver.get("http://" + InetAddressUtils.getInet4Address() + "/#/workspace");
		} catch (SocketException e) {
			throw new IllegalStateException(e);
		}
		this.errListener = errListener;
		this.webDriver = webDriver;
	}

	public void createCtpGateway(String username, String password, String brokerType) {
		webDriver.findElement(By.className("el-table__header-wrapper")).findElement(By.className("el-button--primary")).click();	// 点击【新建】
		WebElement gatewayForm = webDriver.findElement(By.id("gatewayForm"));
		gatewayForm.findElement(By.id("gatewayTypeOptions")).click();	// 展开【网关类型】选项
		Delay.with(200);
		List<WebElement> dropdownWrappers = webDriver.findElements(By.className("el-select-dropdown"));
		for(WebElement dropdown : dropdownWrappers) {
			if(dropdown.isDisplayed()) {				
				dropdown.findElements(By.className("el-select-dropdown__item")).get(0).click();	// 选中【CTP】
			}
		}
		
		if(StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password) && StringUtils.isNotBlank(brokerType)) {			
			gatewayForm.findElement(By.id("gatewaySettings")).click();	// 点击【网关配置】
			
			WebElement ctpForm = webDriver.findElement(By.id("ctpForm"));
			ctpForm.findElements(By.tagName("input")).get(0).sendKeys(username);		// 输入用户名
			ctpForm.findElements(By.tagName("input")).get(1).sendKeys(password);		// 输入密码
			ctpForm.findElement(By.id("brokerOptions")).click();					// 展开【期货公司】
			int i = "main".equals(brokerType) ? 0 : 1;	
			webDriver.findElements(By.className("el-select-dropdown__list")).get(2)
			.findElements(By.className("el-select-dropdown__item")).get(i).click();		// 选中期货公司
			ctpForm.findElement(By.id("saveCtpForm")).click();			// 【保存】网关配置
		}
		Delay.with(100);
		gatewayForm.findElement(By.id("saveGatewaySettings")).click();		// 点击【保存】
		handleIfError();
	}
	
	public void modifyCtpGateway(String username, String password, String brokerType) {
		
	}
	
	public void removeCtpGateway() {
		WebElement row = getCtpGateway();
		stopAndDelete(row);
		handleIfError();
	}
	
	public WebElement getCtpGateway() {
		return findRow("CTP行情");
	}
	
	public void createSimGateway() {
		webDriver.findElement(By.className("el-table__header-wrapper")).findElement(By.className("el-button--primary")).click();	// 点击【新建】
		WebElement gatewayForm = webDriver.findElement(By.id("gatewayForm"));
		gatewayForm.findElement(By.id("gatewayTypeOptions")).click();	// 展开【网关类型】选项
		Delay.with(200);
		List<WebElement> dropdownWrappers = webDriver.findElements(By.className("el-select-dropdown"));
		for(WebElement dropdown : dropdownWrappers) {
			if(dropdown.isDisplayed()) {				
				dropdown.findElements(By.className("el-select-dropdown__item")).get(1).click();	// 选中【SIM】
			}
		}
		gatewayForm.findElement(By.id("saveGatewaySettings")).click();		// 点击【保存】
		handleIfError();
	}
	
	public void removeSimGateway() {
		WebElement row = getSimGateway();
		stopAndDelete(row);
		handleIfError();
	}
	
	public WebElement getSimGateway() {
		return findRow("SIM行情");
	}
	
	private void stopAndDelete(WebElement row) {
		while(row.findElements(By.className("el-button--danger")).size() < 2) {
			Delay.with(200);
		}
		row.findElements(By.className("el-button--danger")).get(0).click();
		do{
			Delay.with(200);
		}while(row.findElements(By.className("el-button--success")).isEmpty());
		Delay.with(200);
		row.findElements(By.className("el-button--danger")).get(0).click();
		Delay.with(200);
		findDisplayedElementFromElements(By.className("el-popconfirm__action"))
			.findElement(By.className("el-button--primary")).click();
	}
	
	private WebElement findDisplayedElementFromElements(By by) {
		List<WebElement> list = webDriver.findElements(by);
		for(WebElement elem : list) {
			if(elem.isDisplayed()) {
				return elem;
			}
		}
		return null;
	}
	
	private WebElement findRow(String gatewayId) {
		List<WebElement> rowsBefore = webDriver.findElements(By.className("el-table__row"));
		for(WebElement row : rowsBefore) {
			List<WebElement> cells = row.findElements(By.tagName("td"));
			try {
				cells.get(0).getText().contains(gatewayId);
			} catch (Exception e) {
				continue;
			}
			return row;
		}
		return null;
	}
	
	private void handleIfError() {
		try {
			WebElement errHint = webDriver.findElement(By.className("el-message--error"));
			errListener.onError(errHint.getText()); 
		} catch (NoSuchElementException e) {
			// 无须处理
		}
	}
}
