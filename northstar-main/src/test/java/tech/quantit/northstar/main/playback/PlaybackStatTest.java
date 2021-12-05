package tech.quantit.northstar.main.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.strategy.api.model.ModuleDealRecord;

public class PlaybackStatTest {
	
	List<ModuleDealRecord> dealRecords;
	
	PlaybackDescription playbackDescription;
	
	PlaybackStat stat;
	
	@BeforeEach
	public void prepare() {
		playbackDescription = PlaybackDescription.builder()
				.fee(1)
				.startDate("20211101")
				.endDate("20220131")
				.build();
		ModuleDealRecord r1 = ModuleDealRecord.builder()
				.closeProfit(-100)
				.volume(2)
				.estimatedOccupiedMoney(1000)
				.build();
		
		ModuleDealRecord r2 = ModuleDealRecord.builder()
				.closeProfit(-100)
				.volume(2)
				.estimatedOccupiedMoney(1000)
				.build();
		
		ModuleDealRecord r3 = ModuleDealRecord.builder()
				.closeProfit(100)
				.volume(2)
				.estimatedOccupiedMoney(1000)
				.build();
		
		ModuleDealRecord r4 = ModuleDealRecord.builder()
				.closeProfit(200)
				.volume(2)
				.estimatedOccupiedMoney(1000)
				.build();
		
		ModuleDealRecord r5 = ModuleDealRecord.builder()
				.closeProfit(90)
				.volume(1)
				.estimatedOccupiedMoney(1200)
				.build();
		
		ModuleDealRecord r6 = ModuleDealRecord.builder()
				.closeProfit(500)
				.volume(2)
				.estimatedOccupiedMoney(1000)
				.build();
		
		ModuleDealRecord r7 = ModuleDealRecord.builder()
				.closeProfit(-300)
				.volume(2)
				.estimatedOccupiedMoney(1500)
				.build();
		
		ModuleDealRecord r8 = ModuleDealRecord.builder()
				.closeProfit(-10)
				.volume(1)
				.estimatedOccupiedMoney(1000)
				.build();
		
		ModuleDealRecord r9 = ModuleDealRecord.builder()
				.closeProfit(100)
				.volume(2)
				.estimatedOccupiedMoney(1000)
				.build();
		
		ModuleDealRecord r10 = ModuleDealRecord.builder()
				.closeProfit(50)
				.volume(2)
				.estimatedOccupiedMoney(1000)
				.build();
		
		
		stat = new PlaybackStat(playbackDescription, List.of(r1,r2,r3,r4,r5,r6,r7,r8,r9,r10));
	}

	@Test
	public void testSumOfProfit() {
		assertThat(stat.sumOfProfit()).isEqualTo(530);
	}

	@Test
	public void testSumOfCommission() {
		assertThat(stat.sumOfCommission()).isEqualTo(18);
	}

	@Test
	public void testTimesOfTransaction() {
		assertThat(stat.timesOfTransaction()).isEqualTo(10);
	}

	@Test
	public void testDuration() {
		assertThat(stat.duration()).isEqualTo(92);
	}

	@Test
	public void testYearlyEarningRate() {
		assertThat(stat.yearlyEarningRate()).isCloseTo(1.898415, offset(1e-6));
	}
	
	@Test
	public void testStdOfPlaybackProfits() {
		assertThat(stat.stdOfPlaybackProfits()).isCloseTo(211.505188, offset(1e-6));
	}

	@Test
	public void testMeanOfNTransactionsAvgProfit() {
		assertThat(stat.meanOfNTransactionsAvgProfit(3)).isCloseTo(70.416667, offset(1e-6));
	}

	@Test
	public void testStdOfNTransactionsAvgProfit() {
		assertThat(stat.stdOfNTransactionsAvgProfit(3)).isCloseTo(58.783411, offset(1e-6));
	}

	@Test
	public void testMeanOfNTransactionsAvgWinningRate() {
		assertThat(stat.meanOfNTransactionsAvgWinningRate(3)).isCloseTo(0.625, offset(1e-6));
	}

	@Test
	public void testStdOfNTransactionsAvgWinningRate() {
		assertThat(stat.stdOfNTransactionsAvgWinningRate(3)).isCloseTo(0.160604, offset(1e-6));
	}

	@Test
	public void testMaxFallback() {
		assertThat(stat.maxFallback()).isEqualTo(310);
	}

	@Test
	public void testMeanOfOccupiedMoney() {
		assertThat(stat.meanOfOccupiedMoney()).isEqualTo(1070);
	}
}
