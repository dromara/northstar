package org.dromara.northstar.strategy.api.utils.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dromara.northstar.strategy.api.utils.bar.BarMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.gateway.api.domain.time.GenericTradeTime;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

class BarMergerTest {
	
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
		BarMerger bm = new BarMerger(2, c, (merger,bar) -> results.add(bar));
		Random rand = new Random();
		LocalTime t = LocalTime.of(0, 1);
		for(int i=0; i<21; i++) {
			BarField bar = BarField.newBuilder()
					.setUnifiedSymbol("rb2205@SHFE@FUTURES")
					.setActionDay(String.valueOf(i))
					.setActionTime(t.plusMinutes(i).format(DateTimeConstant.T_FORMAT_FORMATTER))
					.setActionTimestamp(i)
					.setTradingDay("20220426")
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
		}

		assertThat(results).hasSize(10);
		assertThat(results.get(9).getActionDay()).isEqualTo(samples.get(20).getActionDay());
		assertThat(results.get(9).getActionTime()).isEqualTo(samples.get(20).getActionTime());
		assertThat(results.get(9).getActionTimestamp()).isEqualTo(samples.get(20).getActionTimestamp());
		assertThat(results.get(9).getOpenPrice()).isCloseTo(samples.get(19).getOpenPrice(), offset(1e-6));
		assertThat(results.get(9).getClosePrice()).isCloseTo(samples.get(20).getClosePrice(), offset(1e-6));
		assertThat(results.get(9).getHighPrice()).isCloseTo(Math.max(samples.get(19).getHighPrice(), samples.get(20).getHighPrice()) , offset(1e-6));
		assertThat(results.get(9).getLowPrice()).isCloseTo(Math.min(samples.get(19).getLowPrice(), samples.get(20).getLowPrice()), offset(1e-6));
		assertThat(results.get(9).getVolume()).isEqualTo(samples.get(19).getVolume() + samples.get(20).getVolume());
		assertThat(results.get(9).getNumTrades()).isEqualTo(samples.get(19).getNumTrades() + samples.get(20).getNumTrades());
		assertThat(results.get(9).getOpenInterest()).isCloseTo(samples.get(20).getOpenInterest(), offset(1e-6));
		assertThat(results.get(9).getTurnover()).isCloseTo(samples.get(19).getTurnover() + samples.get(20).getTurnover(), offset(1e-6));
		assertThat(results.get(9).getVolumeDelta()).isEqualTo(samples.get(19).getVolumeDelta() + samples.get(20).getVolumeDelta());
		assertThat(results.get(9).getNumTradesDelta()).isEqualTo(samples.get(19).getNumTradesDelta() + samples.get(20).getNumTradesDelta());
		assertThat(results.get(9).getOpenInterestDelta()).isCloseTo(samples.get(19).getOpenInterestDelta() + samples.get(20).getOpenInterestDelta(), offset(1e-6));
		assertThat(results.get(9).getTurnoverDelta()).isCloseTo(samples.get(19).getTurnoverDelta() + samples.get(20).getTurnoverDelta(), offset(1e-6));
	}

}
