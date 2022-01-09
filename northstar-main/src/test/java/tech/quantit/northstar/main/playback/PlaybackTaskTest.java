package tech.quantit.northstar.main.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.protobuf.InvalidProtocolBufferException;

import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.domain.strategy.StrategyModule;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public class PlaybackTaskTest {
	
	TickField tck1 = TickField.newBuilder()
			.setActionTime("1")
			.setActionTimestamp(1634087280000L + 1000)
			.build();
	
	TickField tck2 = TickField.newBuilder()
			.setActionTime("2")
			.setActionTimestamp(1634087280000L + 2000)
			.build();
	
	TickField tck3 = TickField.newBuilder()
			.setActionTime("3")
			.setActionTimestamp(1634087280000L + 3000)
			.build();
	
	TickField tck4 = TickField.newBuilder()
			.setActionTime("4")
			.setActionTimestamp(1634087340000L + 1000)
			.build();
	
	TickField tck5 = TickField.newBuilder()
			.setActionTime("5")
			.setActionTimestamp(1634087340000L + 2000)
			.build();
	
	TickField tck6 = TickField.newBuilder()
			.setActionTime("6")
			.setActionTimestamp(1634087340000L + 3000)
			.build();
	
	BarField bar1 = BarField.newBuilder()
			.setUnifiedSymbol("rb2205@SHFE@FUTURES")
			.setGatewayId("testGateway")
			.setActionDay("20211111")
			.setTradingDay("20211111")
			.setActionTime("225500")
			.setActionTimestamp(1634087280000L)
			.build();
	
	MinBarDataPO po1 = MinBarDataPO.builder()
			.unifiedSymbol("rb2205@SHFE@FUTURES")
			.gatewayId("testGateway")
			.barData(bar1.toByteArray())
			.ticksData(List.of(tck1, tck2, tck3).stream().map(TickField::toByteArray).toList())
			.updateTime(1634087280000L)
			.build();
	
	BarField bar2 = BarField.newBuilder()
			.setUnifiedSymbol("rb2205@SHFE@FUTURES")
			.setGatewayId("testGateway")
			.setActionDay("20211111")
			.setTradingDay("20211111")
			.setActionTime("225600")
			.setActionTimestamp(1634087340000L)
			.build();
	
	MinBarDataPO po2 = MinBarDataPO.builder()
			.unifiedSymbol("rb2205@SHFE@FUTURES")
			.gatewayId("testGateway")
			.barData(bar2.toByteArray())
			.ticksData(List.of(tck4, tck5, tck6).stream().map(TickField::toByteArray).toList())
			.updateTime(1634087340000L)
			.build();
	
	BarField bar3 = BarField.newBuilder()
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setGatewayId("testGateway")
			.setActionDay("20211111")
			.setTradingDay("20211111")
			.setActionTime("225500")
			.setActionTimestamp(1634087280000L)
			.build();
	
	MinBarDataPO po3 = MinBarDataPO.builder()
			.unifiedSymbol("rb2210@SHFE@FUTURES")
			.gatewayId("testGateway")
			.barData(bar3.toByteArray())
			.ticksData(List.of(tck1, tck2, tck3).stream().map(TickField::toByteArray).toList())
			.updateTime(1634087280000L)
			.build();
	
	BarField bar4 = BarField.newBuilder()
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setGatewayId("testGateway")
			.setActionDay("20211111")
			.setTradingDay("20211111")
			.setActionTime("225600")
			.setActionTimestamp(1634087340000L)
			.build();
	
	MinBarDataPO po4 = MinBarDataPO.builder()
			.unifiedSymbol("rb2210@SHFE@FUTURES")
			.gatewayId("testGateway")
			.barData(bar4.toByteArray())
			.ticksData(List.of(tck4, tck5, tck6).stream().map(TickField::toByteArray).toList())
			.updateTime(1634087340000L)
			.build();

	@Test
	public void test() throws InvalidProtocolBufferException {
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
		
		assertThat(task.hasMoreDayToPlay()).isTrue();
		assertThat(task.ratioOfProcess()).isZero();
		while(task.hasMoreDayToPlay()) {
			assertThat(task.nextBatchDataOfDay().size() > 0).isTrue();
			while(!task.barQ.isEmpty()) {
				task.barQ.poll();
			}
			while(!task.tickQ.isEmpty()) {
				task.tickQ.poll();
			}
		}
		assertThat(task.hasMoreDayToPlay()).isFalse();
		assertThat(task.ratioOfProcess()).isEqualTo(1);
	}
	
	@Test
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
		
		assertThrows(IllegalStateException.class, ()->{			
			task.nextBatchDataOfDay();
		});
	}
	
	@SuppressWarnings("unchecked")
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
		
		when(task.barQ.size()).thenReturn(400);
		task.curDate = LocalDate.of(2021, 1, 10);
		task.totalNumOfData = 500;
		assertThat(task.ratioOfProcess()).isCloseTo(0.920, offset(1e-6));
		
		when(task.barQ.size()).thenReturn(0);
		task.totalNumOfData = 0;
		task.curDate = LocalDate.of(2021, 1, 11);
		assertThat(task.ratioOfProcess()).isCloseTo(1, offset(1e-6));
	}

}
