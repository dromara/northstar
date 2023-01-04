package tech.quantit.northstar.strategy.api.utils.bar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.gateway.api.domain.time.GenericTradeTime;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

class WeeklyBarMergerTest {

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
		BarMerger bm = new WeeklyBarMerger(1, c, bar -> results.add(bar));
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
		
		assertThat(results).hasSize(3);
		assertThat(results.get(1).getActionDay()).isEqualTo(samples.get(8).getActionDay());
		assertThat(results.get(1).getActionTime()).isEqualTo(samples.get(8).getActionTime());
		assertThat(results.get(1).getActionTimestamp()).isEqualTo(samples.get(8).getActionTimestamp());
		assertThat(results.get(1).getOpenPrice()).isCloseTo(samples.get(2).getOpenPrice(), offset(1e-6));
		assertThat(results.get(1).getClosePrice()).isCloseTo(samples.get(8).getClosePrice(), offset(1e-6));
		double highest = StatUtils.max(new double[] {
				samples.get(2).getHighPrice(),
				samples.get(3).getHighPrice(),
				samples.get(4).getHighPrice(),
				samples.get(5).getHighPrice(),
				samples.get(6).getHighPrice(),
				samples.get(7).getHighPrice(),
				samples.get(8).getHighPrice(),
		});
		double lowest = StatUtils.min(new double[] {
				samples.get(2).getLowPrice(),
				samples.get(3).getLowPrice(),
				samples.get(4).getLowPrice(),
				samples.get(5).getLowPrice(),
				samples.get(6).getLowPrice(),
				samples.get(7).getLowPrice(),
				samples.get(8).getLowPrice(),
		});
		double sumVol = StatUtils.sum(new double[] {
				samples.get(2).getVolume(),
				samples.get(3).getVolume(),
				samples.get(4).getVolume(),
				samples.get(5).getVolume(),
				samples.get(6).getVolume(),
				samples.get(7).getVolume(),
				samples.get(8).getVolume(),
		});
		double sumVolDelta = StatUtils.sum(new double[] {
				samples.get(2).getVolumeDelta(),
				samples.get(3).getVolumeDelta(),
				samples.get(4).getVolumeDelta(),
				samples.get(5).getVolumeDelta(),
				samples.get(6).getVolumeDelta(),
				samples.get(7).getVolumeDelta(),
				samples.get(8).getVolumeDelta(),
		});
		double sumOpenInterestDelta = StatUtils.sum(new double[] {
				samples.get(2).getOpenInterestDelta(),
				samples.get(3).getOpenInterestDelta(),
				samples.get(4).getOpenInterestDelta(),
				samples.get(5).getOpenInterestDelta(),
				samples.get(6).getOpenInterestDelta(),
				samples.get(7).getOpenInterestDelta(),
				samples.get(8).getOpenInterestDelta(),
		});
		
		assertThat(results.get(1).getHighPrice()).isCloseTo(highest, offset(1e-6));
		assertThat(results.get(1).getLowPrice()).isCloseTo(lowest, offset(1e-6));
		assertThat(results.get(1).getVolume()).isEqualTo((long)sumVol);
		assertThat(results.get(1).getVolumeDelta()).isEqualTo((long)sumVolDelta);
		assertThat(results.get(1).getOpenInterest()).isCloseTo(samples.get(8).getOpenInterest(), offset(1e-6));
		assertThat(results.get(1).getOpenInterestDelta()).isCloseTo(sumOpenInterestDelta, offset(1e-6));
	}

}
