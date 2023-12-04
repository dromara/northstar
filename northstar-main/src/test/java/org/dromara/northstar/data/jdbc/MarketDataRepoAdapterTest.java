package org.dromara.northstar.data.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class MarketDataRepoAdapterTest {

	IMarketDataRepository repo;

	@Autowired
	MarketDataRepository delegate;

	Contract c = Contract.builder().unifiedSymbol("rb2210@SHFE@FUTURES").build();

	LocalDate date = LocalTime.now().isAfter(LocalTime.of(20, 0))
			? LocalDate.now().plusDays(1)
			: LocalDate.now();

	Bar bar1 = Bar.builder()
			.gatewayId("CTP")
			.contract(c)
			.actionDay(LocalDate.now())
			.actionTime(LocalTime.of(21, 0))
			.tradingDay(date)
			.actionTimestamp(System.currentTimeMillis())
			.openPrice(1)
			.highPrice(2)
			.lowPrice(3)
			.closePrice(4)
			.openInterest(5)
			.openInterestDelta(6)
			.volume(7)
			.volumeDelta(8)
			.turnover(9)
			.turnoverDelta(10)
			.preClosePrice(11)
			.preOpenInterest(12)
			.preSettlePrice(13)
			.channelType(ChannelType.SIM)
			.build();
	Bar bar2 = Bar.builder()
			.gatewayId("CTP")
			.contract(c)
			.actionDay(LocalDate.now())
			.actionTime(LocalTime.of(21, 1))
			.tradingDay(date)
			.channelType(ChannelType.SIM)
			.build();
	Bar bar3 = Bar.builder()
			.gatewayId("CTP")
			.contract(c)
			.actionDay(LocalDate.now())
			.actionTime(LocalTime.of(21, 2))
			.tradingDay(date)
			.channelType(ChannelType.SIM)
			.build();

	@BeforeEach
	void prepare() {
		IContractManager contractMgr = mock(IContractManager.class);
		IContract contract = mock(IContract.class);
		when(contract.contract()).thenReturn(c);
		when(contractMgr.getContract(any(Identifier.class))).thenReturn(contract);
		repo = new MarketDataRepoAdapter(delegate, contractMgr);
	}

	@Test
	void testInsert() {
		IContract contract = mock(IContract.class);
		when(contract.contract()).thenReturn(c);

		repo.insert(bar1);
		repo.insert(bar2);
		repo.insert(bar3);

		List<Bar> results = repo.loadBars(contract, LocalDate.now(), LocalDate.now().plusDays(7));
		assertThat(results).hasSize(3);
		assertThat(results.get(0)).isEqualTo(bar1);
		assertThat(results.get(1)).isEqualTo(bar2);
		assertThat(results.get(2)).isEqualTo(bar3);
	}

}
