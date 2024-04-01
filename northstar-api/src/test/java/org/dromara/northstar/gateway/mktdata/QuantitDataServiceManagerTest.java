package org.dromara.northstar.gateway.mktdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.dromara.northstar.common.model.ResultSet;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

class QuantitDataServiceManagerTest {
	
	RestTemplate restTemplate = new RestTemplateBuilder()
			.rootUri("https://marketplace.quantit.tech")
			.defaultHeader("Authorization", String.format("Bearer %s", System.getenv("NS_DS_SECRET")))
			.build();

	QuantitDataServiceManager mgr = new QuantitDataServiceManager(restTemplate);
	
	@Test
	void testGetAllFutureContracts() {
		ResultSet rs = mgr.getAllFutureContracts();
		assertThat(rs).isNotNull();
		assertThat(rs.size()).isPositive();
	}

	@Test
	void testGetCalendarCN() {
		ResultSet rs = mgr.getCalendarCN(2024);
		assertThat(rs).isNotNull();
		assertThat(rs.size()).isPositive();
	}

	@Test
	void testGetMinutelyData() {
		ResultSet rs = mgr.getMinutelyData("rb0000@SHFE@FUTURES", LocalDate.now().minusWeeks(1), LocalDate.now());
		assertThat(rs).isNotNull();
		assertThat(rs.size()).isPositive();
	}

	@Test
	void testGetQuarterlyData() {
		ResultSet rs = mgr.getQuarterlyData("rb0000@SHFE@FUTURES", LocalDate.now().minusWeeks(1), LocalDate.now());
		assertThat(rs).isNotNull();
		assertThat(rs.size()).isPositive();
	}

	@Test
	void testGetHourlyData() {
		ResultSet rs = mgr.getHourlyData("rb0000@SHFE@FUTURES", LocalDate.now().minusWeeks(1), LocalDate.now());
		assertThat(rs).isNotNull();
		assertThat(rs.size()).isPositive();
	}

	@Test
	void testGetDailyData() {
		ResultSet rs = mgr.getDailyData("rb0000@SHFE@FUTURES", LocalDate.now().minusWeeks(1), LocalDate.now());
		assertThat(rs).isNotNull();
		assertThat(rs.size()).isPositive();
	}

}
