package org.dromara.northstar.gateway.mktdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.datasource.dto.DataSourceMessage.AggBarDTO;
import org.dromara.northstar.datasource.dto.DataSourceMessage.ContractDTO;
import org.dromara.northstar.datasource.dto.DataSourceMessage.HolidayDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

class QuantitDataServiceManagerTest {
	
	RestTemplate restTemplate = new RestTemplateBuilder()
			.rootUri("https://marketplace.quantit.tech")
//			.rootUri("http://127.0.0.1:443")
			.defaultHeader("Authorization", String.format("Bearer %s", System.getenv("NS_DS_SECRET")))
			.build();

	QuantitDataServiceManager mgr = new QuantitDataServiceManager(restTemplate);
	
	@Test
	void testGetCnFutureContracts() {
		List<ContractDTO> rs = mgr.getCnFutureContracts();
		assertThat(rs).isNotEmpty();
		for(ContractDTO dto : rs) {
			assertThat(dto.getOnboardingDate()).isNotBlank();
			assertThat(dto.getLastTradeDate()).isNotBlank();
		}
	}
	
	@Test
	void testGetUsStocksContracts() {
		List<ContractDTO> rs = mgr.getUsStocksContracts();
		assertThat(rs).isNotEmpty();
	}
	
	@Test
	void testGetForexContracts() {
		List<ContractDTO> rs = mgr.getForexContracts();
		assertThat(rs).isNotEmpty();
	}
	
	@Test
	void testGetCryptoContracts() {
		List<ContractDTO> rs = mgr.getCryptoContracts();
		assertThat(rs).isNotEmpty();
		rs.forEach(c -> assertThat(c.getUsingStartTimeInBar()).isTrue());
	}
	
	@Test
	void testGetCalendarCN() {
		List<HolidayDTO> rs = mgr.getHolidays(2024);
		assertThat(rs).isNotNull();
		assertThat(rs.size()).isPositive();
	}

	@Test
	void testGetMinutelyDataFuture() {
		List<AggBarDTO> rs = mgr.getMinutelyData("rb0000@SHFE@FUTURES", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
		AggBarDTO dto = rs.getLast();
		assertThat(dto.getUnifiedSymbol()).isEqualTo("rb0000@SHFE@FUTURES");
		assertThat(CommonUtils.millsToLocalDateTime(dto.getTimestamp()).toLocalTime()).isEqualTo("21:00");
	}
	
	@Test
	void testGetMinutelyDataForex() {
		List<AggBarDTO> rs = mgr.getMinutelyData("CNYUSD@FXCM@FOREX", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
		AggBarDTO dto = rs.getLast();
		assertThat(dto.getUnifiedSymbol()).isEqualTo("CNYUSD@FXCM@FOREX");
		assertThat(CommonUtils.millsToLocalDateTime(dto.getTimestamp()).toLocalTime()).isEqualTo("08:00");
	}
	
	@Test
	void testGetMinutelyDataUsStocks() {
		List<AggBarDTO> rs = mgr.getMinutelyData("AAPL@NSDQX@EQUITY", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
		AggBarDTO dto = rs.getLast();
		AggBarDTO dto2 = rs.get(rs.size() - 2);
		assertThat(dto.getUnifiedSymbol()).isEqualTo("AAPL@NSDQX@EQUITY");
		assertThat(CommonUtils.millsToLocalDateTime(dto.getTimestamp()).toLocalTime()).isEqualTo("21:35");
		assertThat(CommonUtils.millsToLocalDateTime(dto2.getTimestamp()).toLocalTime()).isEqualTo("21:40");
	}
	
	@Test
	void testGetMinutelyDataCrypto() {
		List<AggBarDTO> rs = mgr.getMinutelyData("BTCUSDT@BINANCE@SPOT", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
		AggBarDTO dto = rs.getLast();
		assertThat(dto.getUnifiedSymbol()).isEqualTo("BTCUSDT@BINANCE@SPOT");
		assertThat(CommonUtils.millsToLocalDateTime(dto.getTimestamp()).toLocalTime()).isEqualTo("08:00");
	}

	@Test
	void testGetQuarterlyDataFuture() {
		List<AggBarDTO> rs = mgr.getQuarterlyData("rb0000@SHFE@FUTURES", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
	}

	@Test
	void testGetHourlyDataFuture() {
		List<AggBarDTO> rs = mgr.getHourlyData("rb0000@SHFE@FUTURES", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
		AggBarDTO dto0 = rs.getLast();
		AggBarDTO dto1 = rs.get(rs.size() - 2);
		assertThat(dto0.getUnifiedSymbol()).isEqualTo("rb0000@SHFE@FUTURES");
		assertThat(CommonUtils.millsToLocalDateTime(dto0.getTimestamp()).toLocalTime()).isEqualTo("21:00");
		assertThat(CommonUtils.millsToLocalDateTime(dto1.getTimestamp()).toLocalTime()).isEqualTo("22:00");
	}
	
	@Test
	void testGetHourlyDataForex() {
		List<AggBarDTO> rs = mgr.getHourlyData("CNYUSD@FXCM@FOREX", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
		AggBarDTO dto0 = rs.getLast();
		AggBarDTO dto1 = rs.get(rs.size() - 2);
		assertThat(dto0.getUnifiedSymbol()).isEqualTo("CNYUSD@FXCM@FOREX");
		assertThat(CommonUtils.millsToLocalDateTime(dto0.getTimestamp()).toLocalTime()).isEqualTo("08:00");
		assertThat(CommonUtils.millsToLocalDateTime(dto1.getTimestamp()).toLocalTime()).isEqualTo("09:00");
	}
	
	@Test
	void testGetHourlyDataUsStocks() {
		List<AggBarDTO> rs = mgr.getHourlyData("AAPL@NSDQX@EQUITY", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
		AggBarDTO dto0 = rs.getLast();
		AggBarDTO dto1 = rs.get(rs.size() - 2);
		assertThat(dto0.getUnifiedSymbol()).isEqualTo("AAPL@NSDQX@EQUITY");
		for(AggBarDTO dto : rs) {
			System.out.println(CommonUtils.millsToLocalDateTime(dto.getTimestamp()));
		}
		assertThat(CommonUtils.millsToLocalDateTime(dto0.getTimestamp()).toLocalTime()).isEqualTo("22:30");
		assertThat(CommonUtils.millsToLocalDateTime(dto1.getTimestamp()).toLocalTime()).isEqualTo("23:30");
	}
	
	@Test
	void testGetHourlyDataCrypto() {
		List<AggBarDTO> rs = mgr.getHourlyData("BTCUSDT@BINANCE@SPOT", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
		AggBarDTO dto0 = rs.getLast();
		AggBarDTO dto1 = rs.get(rs.size() - 2);
		assertThat(dto0.getUnifiedSymbol()).isEqualTo("BTCUSDT@BINANCE@SPOT");
		assertThat(CommonUtils.millsToLocalDateTime(dto0.getTimestamp()).toLocalTime()).isEqualTo("08:00");
		assertThat(CommonUtils.millsToLocalDateTime(dto1.getTimestamp()).toLocalTime()).isEqualTo("09:00");
	}

	@Test
	void testGetDailyDataFuture() {
		List<AggBarDTO> rs = mgr.getDailyData("rb0000@SHFE@FUTURES", LocalDate.of(2024, 10, 10), LocalDate.of(2024, 10, 13));
		assertThat(rs).isNotEmpty();
	}

}
