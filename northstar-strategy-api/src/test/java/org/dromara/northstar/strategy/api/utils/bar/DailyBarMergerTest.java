package org.dromara.northstar.strategy.api.utils.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.common.domain.contract.Contract;
import org.dromara.northstar.gateway.common.domain.time.GenericTradeTime;
import org.dromara.northstar.strategy.api.utils.bar.BarMerger;
import org.dromara.northstar.strategy.api.utils.bar.DailyBarMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

class DailyBarMergerTest {

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
		BarMerger bm = new DailyBarMerger(2, c, (merger,bar) -> results.add(bar));
		Random rand = new Random();
		LocalDate date = LocalDate.now();
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
		
		assertThat(results).hasSize(10);
		assertThat(results.get(9).getActionDay()).isEqualTo(samples.get(19).getActionDay());
		assertThat(results.get(9).getActionTime()).isEqualTo(samples.get(19).getActionTime());
		assertThat(results.get(9).getActionTimestamp()).isEqualTo(samples.get(19).getActionTimestamp());
		assertThat(results.get(9).getOpenPrice()).isCloseTo(samples.get(18).getOpenPrice(), offset(1e-6));
		assertThat(results.get(9).getClosePrice()).isCloseTo(samples.get(19).getClosePrice(), offset(1e-6));
		assertThat(results.get(9).getHighPrice()).isCloseTo(Math.max(samples.get(18).getHighPrice(), samples.get(19).getHighPrice()) , offset(1e-6));
		assertThat(results.get(9).getLowPrice()).isCloseTo(Math.min(samples.get(18).getLowPrice(), samples.get(19).getLowPrice()), offset(1e-6));
		assertThat(results.get(9).getVolume()).isEqualTo(samples.get(18).getVolume() + samples.get(19).getVolume());
		assertThat(results.get(9).getNumTrades()).isEqualTo(samples.get(18).getNumTrades() + samples.get(19).getNumTrades());
		assertThat(results.get(9).getOpenInterest()).isCloseTo(samples.get(19).getOpenInterest(), offset(1e-6));
		assertThat(results.get(9).getTurnover()).isCloseTo(samples.get(18).getTurnover() + samples.get(19).getTurnover(), offset(1e-6));
		assertThat(results.get(9).getVolumeDelta()).isEqualTo(samples.get(18).getVolumeDelta() + samples.get(19).getVolumeDelta());
		assertThat(results.get(9).getNumTradesDelta()).isEqualTo(samples.get(18).getNumTradesDelta() + samples.get(19).getNumTradesDelta());
		assertThat(results.get(9).getOpenInterestDelta()).isCloseTo(samples.get(18).getOpenInterestDelta() + samples.get(19).getOpenInterestDelta(), offset(1e-6));
		assertThat(results.get(9).getTurnoverDelta()).isCloseTo(samples.get(18).getTurnoverDelta() + samples.get(19).getTurnoverDelta(), offset(1e-6));
	}

}
