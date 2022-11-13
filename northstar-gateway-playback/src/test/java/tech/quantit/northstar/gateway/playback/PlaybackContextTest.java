package tech.quantit.northstar.gateway.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.constant.PlaybackSpeed;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.gateway.playback.utils.PlaybackClock;
import tech.quantit.northstar.gateway.playback.utils.PlaybackDataLoader;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.TickField;

class PlaybackContextTest {
	
	IPlaybackRuntimeRepository rtRepo = mock(IPlaybackRuntimeRepository.class);
	FastEventEngine feEngine = mock(FastEventEngine.class);
	PlaybackDataLoader loader = mock(PlaybackDataLoader.class);
	PlaybackClock clock = mock(PlaybackClock.class);
	
	LocalDateTime ldt = LocalDateTime.of(2022, 6, 29, 9, 0);
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	IContractManager contractMgr = mock(IContractManager.class);
	
	ContractField contract = factory.makeContract("rb2210");
	PlaybackGatewaySettings settings = new PlaybackGatewaySettings();
	
	
	
	TickField t1 = factory.makeTickField("rb2210", 5000);
	TickField t2 = factory.makeTickField("rb2210", 5001);
	TickField t3 = factory.makeTickField("rb2210", 5002);
	TickField t4 = factory.makeTickField("rb2210", 5000);
	
	BarField bar = factory.makeBarField("rb2210", 5000, 20, ldt); 
	
	@BeforeEach
	void prepare() {
		when(clock.nextMarketMinute()).thenReturn(ldt.plusMinutes(1));
		when(loader.loadMinuteData(eq(ldt), eq(contract))).thenReturn(List.of(bar));
		when(loader.loadTradeDayDataRaw(any(LocalDate.class), any(LocalDate.class), eq(contract))).thenReturn(List.of(bar));
		when(contractMgr.getContract(anyString())).thenReturn(contract);
		
		settings.setPreStartDate("20220629");
		settings.setStartDate("20220629");
		settings.setEndDate("20220629");
		settings.setPrecision(PlaybackPrecision.LOW);
		settings.setSpeed(PlaybackSpeed.SPRINT);
		settings.setUnifiedSymbols(List.of(contract.getUnifiedSymbol()));
	}
	
	@Test
	void testRunning() throws InterruptedException {
		PlaybackContext ctx = new PlaybackContext(settings, ldt, clock, loader, feEngine, rtRepo, contractMgr);
		ctx.setGatewaySettings(GatewaySettingField.newBuilder().setGatewayId("testGateway").build());
		
		ctx.start();
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
