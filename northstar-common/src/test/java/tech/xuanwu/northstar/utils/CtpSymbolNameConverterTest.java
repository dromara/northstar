package tech.xuanwu.northstar.utils;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;

public class CtpSymbolNameConverterTest {

	@Test(dataProvider = "dp")
	public void f(String n, String s) {
		assertThat(ContractNameResolver.getCNSymbolName(n)).isEqualTo(s);
	}

	@DataProvider
	public Object[][] dp() {
		return new Object[][] { 
			new Object[] { "rb2010", "螺纹钢2010" }, 
			new Object[] { "AP010", "苹果010" },
			new Object[] { "m2010", "豆粕2010" }, 
			};
	}
}
