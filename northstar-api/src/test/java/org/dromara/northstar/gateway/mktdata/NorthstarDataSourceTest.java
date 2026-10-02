package org.dromara.northstar.gateway.mktdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.CommonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

class NorthstarDataSourceTest {
	
	RestTemplate restTemplate = new RestTemplateBuilder()
			.rootUri("https://marketplace.quantit.tech")
			.defaultHeader("Authorization", String.format("Bearer %s", System.getenv("NS_DS_SECRET")))
			.build();

	QuantitDataServiceManager mgr = new QuantitDataServiceManager(restTemplate);
	
	NorthstarDataSource ds = new NorthstarDataSource(mgr);
	
	Contract c = Contract.builder()
			.unifiedSymbol("rb9999@SHFE@FUTURES")
			.exchange(ExchangeEnum.SHFE)
			.contractDefinition(ContractDefinition.builder()
					.tradeTimeDef(TradeTimeDefinition.builder()
							.timeSlots(List.of(TimeSlot.builder()
									.start(LocalTime.of(9, 0))
									.end(LocalTime.of(15, 0))
									.build()))
							.build())
					.build())
			.symbol("rb9999")
			.build();
	
	@Test
	void testGetMinutelyData() {
		long end = CommonUtils.localDateTimeToMills(LocalDate.of(2026, 6, 11).atStartOfDay());
		long start = CommonUtils.localDateTimeToMills(LocalDate.of(2026, 6, 8).atStartOfDay());
		assertThat(ds.getData(c, Interval.M1, start, end)).isNotEmpty();
	}

	@Test
	void testGetQuarterlyData() {
		long end = CommonUtils.localDateTimeToMills(LocalDate.of(2026, 6, 11).atStartOfDay());
		long start = CommonUtils.localDateTimeToMills(LocalDate.of(2026, 6, 8).atStartOfDay());
		assertThat(ds.getData(c, Interval.M15, start, end)).isNotEmpty();
	}

	@Test
	void testGetHourlyData() {
		long end = CommonUtils.localDateTimeToMills(LocalDate.of(2026, 6, 11).atStartOfDay());
		long start = CommonUtils.localDateTimeToMills(LocalDate.of(2026, 6, 8).atStartOfDay());
		assertThat(ds.getData(c, Interval.H1, start, end)).isNotEmpty();
	}

	@Test
	void testGetDailyData() {
		long end = CommonUtils.localDateTimeToMills(LocalDate.of(2026, 6, 11).atStartOfDay());
		long start = CommonUtils.localDateTimeToMills(LocalDate.of(2026, 6, 8).atStartOfDay());
		assertThat(ds.getData(c, Interval.D1, start, end)).isNotEmpty();
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
