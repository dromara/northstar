package tech.xuanwu.northstar.main.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.junit.Test;

import tech.xuanwu.northstar.common.constant.PlaybackPrecision;
import tech.xuanwu.northstar.common.model.PlaybackDescription;
import tech.xuanwu.northstar.domain.strategy.StrategyModule;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.main.persistence.po.TickDataPO;

public class PlaybackTaskTest {
	
	TickDataPO tck1 = TickDataPO.builder()
			.actionTime("1")
			.actionTimestamp(1634087280000L + 1000)
			.build();
	
	TickDataPO tck2 = TickDataPO.builder()
			.actionTime("2")
			.actionTimestamp(1634087280000L + 2000)
			.build();
	
	TickDataPO tck3 = TickDataPO.builder()
			.actionTime("3")
			.actionTimestamp(1634087280000L + 3000)
			.build();
	
	TickDataPO tck4 = TickDataPO.builder()
			.actionTime("4")
			.actionTimestamp(1634087340000L + 1000)
			.build();
	
	TickDataPO tck5 = TickDataPO.builder()
			.actionTime("5")
			.actionTimestamp(1634087340000L + 2000)
			.build();
	
	TickDataPO tck6 = TickDataPO.builder()
			.actionTime("6")
			.actionTimestamp(1634087340000L + 3000)
			.build();
	
	MinBarDataPO po1 = MinBarDataPO.builder()
			.unifiedSymbol("rb2205@SHFE@FUTURES")
			.gatewayId("testGateway")
			.actionDay("20211111")
			.tradingDay("20211111")
			.actionTime("225500")
			.actionTimestamp(1634087280000L)
			.ticksOfMin(List.of(tck1, tck2, tck3))
			.build();
	
	MinBarDataPO po2 = MinBarDataPO.builder()
			.unifiedSymbol("rb2205@SHFE@FUTURES")
			.gatewayId("testGateway")
			.actionDay("20211111")
			.tradingDay("20211111")
			.actionTime("225600")
			.actionTimestamp(1634087340000L)
			.ticksOfMin(List.of(tck4, tck5, tck6))
			.build();
	
	MinBarDataPO po3 = MinBarDataPO.builder()
			.unifiedSymbol("rb2210@SHFE@FUTURES")
			.gatewayId("testGateway")
			.actionDay("20211111")
			.tradingDay("20211111")
			.actionTime("225500")
			.actionTimestamp(1634087280000L)
			.ticksOfMin(List.of(tck1, tck2, tck3))
			.build();
	
	MinBarDataPO po4 = MinBarDataPO.builder()
			.unifiedSymbol("rb2210@SHFE@FUTURES")
			.gatewayId("testGateway")
			.actionDay("20211111")
			.tradingDay("20211111")
			.actionTime("225600")
			.actionTimestamp(1634087340000L)
			.ticksOfMin(List.of(tck4, tck5, tck6))
			.build();

	@Test
	public void test() {
		StrategyModule module = mock(StrategyModule.class);
		when(module.bindedContractUnifiedSymbols()).thenReturn(Set.of("rb2210@SHFE@FUTURES"));
		when(module.getBindedMktGatewayId()).thenReturn("testGateway");
		PlaybackDescription description = PlaybackDescription.builder()
				.startDate("20210101")
				.endDate("20210131")
				.precision(PlaybackPrecision.TICK)
				.build();
		MarketDataRepository mdRepo = mock(MarketDataRepository.class);
		when(mdRepo.loadDataByDate(anyString(), anyString(), anyString())).thenReturn(List.of(po1,po2,po3,po4));
		PlaybackTask task = new PlaybackTask(description, List.of(module), mdRepo);
		
		assertThat(task.isDone()).isFalse();
		assertThat(task.ratioOfProcess()).isZero();
		while(!task.isDone()) {
			assertThat(task.nextBatchData().size() > 0).isTrue();
		}
		assertThat(task.isDone()).isTrue();
		assertThat(task.ratioOfProcess()).isEqualTo(1);
	}
	
	@Test(expected = IllegalStateException.class)
	public void testException() {
		StrategyModule module = mock(StrategyModule.class);
		when(module.bindedContractUnifiedSymbols()).thenReturn(Set.of("rb2210@SHFE@FUTURES"));
		when(module.getBindedMktGatewayId()).thenReturn("testGateway");
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
	
	@Test
	public void testProcess() {
		StrategyModule module = mock(StrategyModule.class);
		when(module.bindedContractUnifiedSymbols()).thenReturn(Set.of("rb2210@SHFE@FUTURES"));
		when(module.getBindedMktGatewayId()).thenReturn("testGateway");
		PlaybackDescription description = PlaybackDescription.builder()
				.startDate("20210101")
				.endDate("20210110")
				.precision(PlaybackPrecision.TICK)
				.build();
		MarketDataRepository mdRepo = mock(MarketDataRepository.class);
		when(mdRepo.loadDataByDate(anyString(), anyString(), anyString())).thenReturn(List.of(mock(MinBarDataPO.class)));
		PlaybackTask task = new PlaybackTask(description, List.of(module), mdRepo);
		task.barQ = mock(PriorityQueue.class);
		assertThat(task.ratioOfProcess()).isCloseTo(0, offset(1e-6));
		
		when(task.barQ.size()).thenReturn(400);
		task.curDate = LocalDate.of(2021, 1, 2);
		task.totalNumOfData = 500;
		assertThat(task.ratioOfProcess()).isCloseTo(0.120, offset(1e-6));
		
		when(task.barQ.size()).thenReturn(0);
		task.totalNumOfData = 0;
		task.curDate = LocalDate.of(2021, 1, 11);
		assertThat(task.ratioOfProcess()).isCloseTo(1, offset(1e-6));
	}

}
