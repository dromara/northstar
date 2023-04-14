package org.dromara.northstar.gateway.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.dromara.northstar.gateway.playback.PlaybackContext;
import org.dromara.northstar.gateway.playback.PlaybackGatewaySettings;
import org.dromara.northstar.gateway.playback.utils.PlaybackClock;
import org.dromara.northstar.gateway.playback.utils.PlaybackDataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.constant.PlaybackSpeed;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.model.ContractSimpleInfo;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.common.model.Identifier;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.gateway.api.IContractManager;
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

class PlaybackContextTest {
	
	IPlaybackRuntimeRepository rtRepo = mock(IPlaybackRuntimeRepository.class);
	FastEventEngine feEngine = mock(FastEventEngine.class);
	PlaybackDataLoader loader = mock(PlaybackDataLoader.class);
	PlaybackClock clock = mock(PlaybackClock.class);
	
	LocalDateTime ldt = LocalDateTime.of(2022, 6, 29, 21, 0);
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	IContractManager contractMgr = mock(IContractManager.class);
	
	ContractField contract = factory.makeContract("rb2210");
	PlaybackGatewaySettings settings = new PlaybackGatewaySettings();
	
	TickField t1 = factory.makeTickField("rb2210", 5000);
	TickField t2 = factory.makeTickField("rb2210", 5001);
	TickField t3 = factory.makeTickField("rb2210", 5002);
	TickField t4 = factory.makeTickField("rb2210", 5000);
	
	BarField bar = factory.makeBarField("rb2210", 5000, 20, ldt);
	Contract c = mock(Contract.class);
	
	@BeforeEach
	void prepare() {
		when(clock.nextMarketMinute()).thenReturn(ldt.plusMinutes(1));
		when(loader.loadMinuteData(eq(ldt), eq(contract))).thenReturn(List.of(bar));
		when(loader.loadTradeDayDataRaw(any(LocalDate.class), any(LocalDate.class), eq(contract))).thenReturn(List.of(bar));
		when(contractMgr.getContract(any(Identifier.class))).thenReturn(c);
		when(contractMgr.getContract(anyString(), anyString())).thenReturn(c);
		when(c.contractField()).thenReturn(contract);
		
		settings.setPreStartDate("20220629");
		settings.setStartDate("20220629");
		settings.setEndDate("20220629");
		settings.setPrecision(PlaybackPrecision.LOW);
		settings.setSpeed(PlaybackSpeed.SPRINT);
		settings.setPlayContracts(List.of(ContractSimpleInfo.builder().value(contract.getUnifiedSymbol()).build()));
	}
	
	@Test
	void testRunning() throws InterruptedException {
		PlaybackContext ctx = new PlaybackContext(GatewayDescription.builder()
				.gatewayId("testGateway")
				.settings(settings)
				.build(), ldt, clock, loader, feEngine, rtRepo, contractMgr);
		
		assertDoesNotThrow(() -> {
			ctx.start();
			assertThat(ctx.isRunning()).isTrue();
			ctx.stop();
			assertThat(ctx.isRunning()).isFalse();
			Thread.sleep(500);
			ctx.start();
			Thread.sleep(1000);
			ctx.stop();
		});
		
	}

}
