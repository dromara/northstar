package org.dromara.northstar.gateway.mktdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.core.Contract;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

class NorthstarDataSourceTest {
	
	RestTemplate restTemplate = new RestTemplateBuilder()
			.rootUri("https://marketplace.quantit.tech")
			.defaultHeader("Authorization", String.format("Bearer %s", System.getenv("NS_DS_SECRET")))
			.build();

	QuantitDataServiceManager mgr = new QuantitDataServiceManager(restTemplate);
	
	NorthstarDataSource ds = new NorthstarDataSource(mgr);
	
	Contract c = Contract.builder()
			.unifiedSymbol("rb0000@SHFE@FUTURES")
			.build();

	@Test
	void testGetMinutelyData() {
		assertThat(ds.getMinutelyData(c, LocalDate.of(2024, 3, 22), LocalDate.of(2024, 3, 22))).isNotEmpty();
	}

	@Test
	void testGetQuarterlyData() {
		assertThat(ds.getQuarterlyData(c, LocalDate.of(2024, 3, 22), LocalDate.of(2024, 3, 22))).isNotEmpty();
	}

	@Test
	void testGetHourlyData() {
		assertThat(ds.getHourlyData(c, LocalDate.of(2024, 3, 22), LocalDate.of(2024, 3, 22))).isNotEmpty();
	}

	@Test
	void testGetDailyData() {
		assertThat(ds.getDailyData(c, LocalDate.of(2024, 3, 22), LocalDate.of(2024, 3, 22))).isNotEmpty();
	}

	@Test
	void testGetHolidays() {
		assertThat(ds.getHolidays(ChannelType.CTP, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))).isNotEmpty();
	}

	@Test
	void testGetAllContracts() {
		assertThat(ds.getAllContracts()).isNotEmpty();
	}

}
