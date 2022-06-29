package tech.quantit.northstar.gateway.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.constant.PlaybackSpeed;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.PlaybackSettings;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.gateway.playback.ticker.TickSimulationAlgorithm;
import tech.quantit.northstar.gateway.playback.utils.PlaybackClock;
import tech.quantit.northstar.gateway.playback.utils.PlaybackDataLoader;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.TickField;

class PlaybackContextTest {
	
	TickSimulationAlgorithm algo = mock(TickSimulationAlgorithm.class);
	IPlaybackRuntimeRepository rtRepo = mock(IPlaybackRuntimeRepository.class);
	FastEventEngine feEngine = mock(FastEventEngine.class);
	PlaybackDataLoader loader = mock(PlaybackDataLoader.class);
	PlaybackClock clock = mock(PlaybackClock.class);
	
	LocalDateTime ldt = LocalDateTime.of(2022, 6, 29, 9, 0);
	PlaybackSettings settings = PlaybackSettings.builder()
			.startDate("20220629")
			.endDate("20220629")
			.precision(PlaybackPrecision.LOW)
			.speed(PlaybackSpeed.SPRINT)
			.contractGroups(List.of("someContractGroup"))
			.build();
	
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	
	TickField t1 = factory.makeTickField("rb2210", 5000);
	TickField t2 = factory.makeTickField("rb2210", 5001);
	TickField t3 = factory.makeTickField("rb2210", 5002);
	TickField t4 = factory.makeTickField("rb2210", 5000);
	
	BarField bar = factory.makeBarField("rb2210", 5000, 20, ldt); 
	
	@BeforeEach
	void prepare() {
		Map<ContractField, List<BarField>> map = new HashMap<>();
		map.put(factory.makeContract("rb2210"), List.of(bar));
		when(clock.nextMarketMinute()).thenReturn(ldt.plusMinutes(1).toInstant(ZoneOffset.ofHours(8)).toEpochMilli());
		when(loader.loadData(eq(ldt.toInstant(ZoneOffset.ofHours(8)).toEpochMilli()), anyString())).thenReturn(map);
		when(algo.generateFrom(any(BarField.class))).thenReturn(List.of(t1, t2, t3, t4));
		
	}
	
	@Test
	void testRunning() throws InterruptedException {
		PlaybackContext ctx = new PlaybackContext(settings, ldt, clock, algo, loader, feEngine, rtRepo);
		ctx.setGatewaySettings(GatewaySettingField.newBuilder().setGatewayId("testGateway").build());
		
		ctx.start();
		Thread.sleep(100);
		assertThat(ctx.isRunning()).isTrue();
		ctx.stop();
		assertThat(ctx.isRunning()).isFalse();
		Thread.sleep(500);
		ctx.start();
		Thread.sleep(500);
		ctx.stop();
		
		verify(feEngine, times(4)).emitEvent(eq(NorthstarEventType.TICK), any(TickField.class));
		verify(feEngine, times(1)).emitEvent(eq(NorthstarEventType.BAR), any(BarField.class));
		
	}

}
