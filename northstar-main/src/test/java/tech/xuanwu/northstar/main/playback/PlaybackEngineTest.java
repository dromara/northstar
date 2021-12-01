package tech.xuanwu.northstar.main.playback;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.domain.strategy.SandboxModuleManager;
import tech.xuanwu.northstar.domain.strategy.StrategyModule;
import tech.xuanwu.northstar.gateway.sim.trade.SimMarket;
import tech.xuanwu.northstar.main.playback.PlaybackTask.DataType;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

public class PlaybackEngineTest {

	PlaybackEngine engine;
	StrategyModule module = mock(StrategyModule.class);
	PlaybackTask task = mock(PlaybackTask.class);
	Map<DataType, PriorityQueue<?>> batchData = new HashMap<>();
	
	PriorityQueue<TickField> tickQ = new PriorityQueue<>(100000, (t1, t2) -> t1.getActionTimestamp() < t2.getActionTimestamp() ? -1 : 1 );
	PriorityQueue<BarField> barQ = new PriorityQueue<>(3000, (b1, b2) -> b1.getActionTimestamp() < b2.getActionTimestamp() ? -1 : 1 );
	
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
	
	BarField bar2 = BarField.newBuilder()
			.setUnifiedSymbol("rb2205@SHFE@FUTURES")
			.setGatewayId("testGateway")
			.setActionDay("20211111")
			.setTradingDay("20211111")
			.setActionTime("225600")
			.setActionTimestamp(1634087340000L)
			.build();
	
	BarField bar3 = BarField.newBuilder()
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setGatewayId("testGateway")
			.setActionDay("20211111")
			.setTradingDay("20211111")
			.setActionTime("225500")
			.setActionTimestamp(1634087280000L)
			.build();
	
	BarField bar4 = BarField.newBuilder()
			.setUnifiedSymbol("rb2210@SHFE@FUTURES")
			.setGatewayId("testGateway")
			.setActionDay("20211111")
			.setTradingDay("20211111")
			.setActionTime("225600")
			.setActionTimestamp(1634087340000L)
			.build();
	
	@Before
	public void prepare() {
		when(task.isDone()).thenReturn(false, true);
		when(task.nextBatchData()).thenReturn(batchData);
		
		batchData.put(DataType.BAR, barQ);
		batchData.put(DataType.TICK, tickQ);
		
		for(TickField t : List.of(tck6, tck5, tck4, tck3, tck2, tck1)) {
			tickQ.offer(t);
		}
		for(BarField b : List.of(bar2, bar1, bar3, bar4)) {
			barQ.offer(b);
		}
	}
	
	@Test
	public void test() {
		SimMarket market = mock(SimMarket.class);
		SandboxModuleManager moduleMgr = mock(SandboxModuleManager.class);
		engine = new PlaybackEngine(market, moduleMgr);
		engine.play(task);
		
		verify(moduleMgr, times(10)).onEvent(any(NorthstarEvent.class));
	}

}
