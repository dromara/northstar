package tech.xuanwu.northstar.main.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import tech.xuanwu.northstar.common.constant.PlaybackPrecision;
import tech.xuanwu.northstar.common.model.PlaybackDescription;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.strategy.common.StrategyModule;

public class PlaybackTaskTest {

	@Test
	public void test() {
		StrategyModule module = mock(StrategyModule.class);
		when(module.getInterestContractUnifiedSymbol()).thenReturn(Set.of("rb2210@SHFE@FUTURES"));
		when(module.getBindedMarketGatewayId()).thenReturn("testGateway");
		PlaybackDescription description = PlaybackDescription.builder()
				.startDate("20210101")
				.endDate("20210131")
				.precision(PlaybackPrecision.TICK)
				.build();
		MarketDataRepository mdRepo = mock(MarketDataRepository.class);
		when(mdRepo.loadDataByDate(anyString(), anyString(), anyString())).thenReturn(List.of(mock(MinBarDataPO.class)));
		PlaybackTask task = new PlaybackTask(description, List.of(module), mdRepo);
		
		assertThat(task.isDone()).isFalse();
		assertThat(task.ratioOfProcess()).isEqualTo(0);
		while(!task.isDone()) {
			assertThat(task.nextBatchData().size() > 0).isTrue();
		}
		assertThat(task.isDone()).isTrue();
		assertThat(task.ratioOfProcess()).isEqualTo(1);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testException() {
		StrategyModule module = mock(StrategyModule.class);
		when(module.getInterestContractUnifiedSymbol()).thenReturn(Set.of("rb2210@SHFE@FUTURES"));
		when(module.getBindedMarketGatewayId()).thenReturn("testGateway");
		PlaybackDescription description = PlaybackDescription.builder()
				.startDate("20210102")
				.endDate("20210101")
				.precision(PlaybackPrecision.TICK)
				.build();
		MarketDataRepository mdRepo = mock(MarketDataRepository.class);
		when(mdRepo.loadDataByDate(anyString(), anyString(), anyString())).thenReturn(List.of(mock(MinBarDataPO.class)));
		PlaybackTask task = new PlaybackTask(description, List.of(module), mdRepo);
		
		task.nextBatchData();
	}

}
