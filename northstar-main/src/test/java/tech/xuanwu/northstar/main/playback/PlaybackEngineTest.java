package tech.xuanwu.northstar.main.playback;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import tech.xuanwu.northstar.gateway.sim.trade.SimMarket;
import tech.xuanwu.northstar.main.manager.SandboxModuleManager;
import tech.xuanwu.northstar.main.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.main.persistence.po.TickDataPO;
import tech.xuanwu.northstar.strategy.common.StrategyModule;

public class PlaybackEngineTest {

	PlaybackEngine engine;
	StrategyModule module = mock(StrategyModule.class);
	PlaybackTask task = mock(PlaybackTask.class);
	Map<String, Iterator<MinBarDataPO>> batchData = new HashMap<>();
	
	
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
	
	@Before
	public void prepare() {
		when(task.isDone()).thenReturn(false, false, true);
		when(task.nextBatchData()).thenReturn(batchData);
		when(task.getPlaybackModules()).thenReturn(List.of(module));
		
		batchData.put("rb2205@SHFE@FUTURES", List.of(po1, po2).iterator());
		batchData.put("rb2210@SHFE@FUTURES", List.of(po3, po4).iterator());
	}
	
	@Test
	public void test() {
		SimMarket market = mock(SimMarket.class);
		SandboxModuleManager moduleMgr = mock(SandboxModuleManager.class);
		engine = new PlaybackEngine(market, moduleMgr);
		engine.play(task);
		
		verify(moduleMgr, times(12)).onTick(ArgumentMatchers.any());
		verify(moduleMgr, times(4)).onBar(ArgumentMatchers.any());
	}

}
