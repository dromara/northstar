package org.dromara.northstar.support.utils.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.common.domain.time.GenericTradeTime;
import org.dromara.northstar.support.utils.bar.BarMerger;
import org.dromara.northstar.support.utils.bar.MonthlyBarMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

class MonthlyBarMergerTest {

	TestFieldFactory factory = new TestFieldFactory("gateway");
	
	ContractField contract = factory.makeContract("rb2205");
	
	Contract c = mock(Contract.class);
	
	@BeforeEach
	void prepare() {
		when(c.contractField()).thenReturn(contract);
		when(c.tradeTimeDefinition()).thenReturn(new GenericTradeTime());
	}
	
	@Test
	void test() {
		List<BarField> samples = new ArrayList<>();
		List<BarField> results = new ArrayList<>();
		BarMerger bm = new MonthlyBarMerger(1, c, (merger,bar) -> results.add(bar));
		Random rand = new Random();
		LocalDate date = LocalDate.of(2022, 9, 17);
		for(int i=0; i<21; i++) {
			BarField bar = BarField.newBuilder()
					.setUnifiedSymbol("rb2205@SHFE@FUTURES")
					.setActionDay(date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.setActionTime(String.valueOf(i))
					.setActionTimestamp(i)
					.setTradingDay(date.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
					.setOpenPrice(rand.nextDouble(5000))
					.setClosePrice(rand.nextDouble(5000))
					.setHighPrice(rand.nextDouble(5000))
					.setLowPrice(rand.nextDouble(5000))
					.setVolume(rand.nextLong(500000000))
					.setVolumeDelta(rand.nextLong(50000))
					.setOpenInterest(rand.nextDouble(50000000000L))
					.setOpenInterestDelta(rand.nextDouble(5000000))
					.setTurnover(rand.nextDouble(500000000))
					.setTurnoverDelta(rand.nextDouble(50000000))
					.setNumTrades(rand.nextLong(500000000))
					.setNumTradesDelta(rand.nextLong(500000))
					.build();
			bm.onBar(bar);
			samples.add(bar);
			
			date = date.plusDays(1);
		}
		
		assertThat(results).hasSize(1);
		assertThat(results.get(0).getActionDay()).isEqualTo(samples.get(13).getActionDay());
		assertThat(results.get(0).getActionTime()).isEqualTo(samples.get(13).getActionTime());
		assertThat(results.get(0).getActionTimestamp()).isEqualTo(samples.get(13).getActionTimestamp());
		assertThat(results.get(0).getTradingDay()).isEqualTo("20220930");
		assertThat(results.get(0).getOpenPrice()).isCloseTo(samples.get(0).getOpenPrice(), offset(1e-6));
		assertThat(results.get(0).getClosePrice()).isCloseTo(samples.get(13).getClosePrice(), offset(1e-6));
		
	}

}
