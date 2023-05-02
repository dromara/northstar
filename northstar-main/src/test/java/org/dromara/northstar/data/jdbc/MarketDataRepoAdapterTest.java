package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.ds.MarketDataServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;

@DataJpaTest
class MarketDataRepoAdapterTest {
	
	IMarketDataRepository repo;
	
	@Autowired
	MarketDataRepository delegate;

	TestFieldFactory fieldFactory = new TestFieldFactory("test");
	
	String date = LocalTime.now().isAfter(LocalTime.of(20, 0)) 
			? LocalDate.now().plusDays(1).format(DateTimeConstant.D_FORMAT_INT_FORMATTER)
			: LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER);
	
	BarField bar1 = BarField.newBuilder()
			.setGatewayId("CTP")
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setTradingDay(date)
			.build();
	
	BarField bar2 = BarField.newBuilder()
			.setGatewayId("CTP")
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setTradingDay(date)
			.build();
	
	BarField bar3 = BarField.newBuilder()
			.setGatewayId("CTP")
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setTradingDay(date)
			.build();
	
	@BeforeEach
	void prepare() {
		MarketDataServiceImpl dataService =  mock(MarketDataServiceImpl.class);
		when(dataService.loadBars(anyString(), any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
		repo = new MarketDataRepoAdapter(delegate, dataService);
	}
	
	@Test
	void testInsert() {
		repo.insert(bar1);
		repo.insert(bar2);
		repo.insert(bar3);
		
		assertThat(repo.loadBars("rb2210@SHFE@FUTURES", LocalDate.now(), LocalDate.now().plusDays(7))).hasSize(3);
	}

}
