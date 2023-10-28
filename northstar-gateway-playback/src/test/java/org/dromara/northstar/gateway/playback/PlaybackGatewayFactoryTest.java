package org.dromara.northstar.gateway.playback;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.dromara.northstar.common.IHolidayManager;
import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.constant.PlaybackSpeed;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IContractManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.common.TestFieldFactory;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

class PlaybackGatewayFactoryTest {
	
	IPlaybackRuntimeRepository rtRepo = mock(IPlaybackRuntimeRepository.class);
	FastEventEngine feEngine = mock(FastEventEngine.class);
	PlaybackDataServiceManager dsMgr = mock(PlaybackDataServiceManager.class);
	
	LocalDateTime ldt = LocalDateTime.of(2022, 6, 29, 9, 0);
	TestFieldFactory factory = new TestFieldFactory("testGateway");
	IContractManager contractMgr = mock(IContractManager.class);
	
	ContractField contract = factory.makeContract("rb2210");
	PlaybackGatewaySettings settings = new PlaybackGatewaySettings();
	
	IHolidayManager holidayMgr = mock(IHolidayManager.class);
	IMarketDataRepository mdRepo = mock(IMarketDataRepository.class);
	GatewayMetaProvider gatewayMetaProvider = mock(GatewayMetaProvider.class);
	
	TickField t1 = factory.makeTickField("rb2210", 5000);
	TickField t2 = factory.makeTickField("rb2210", 5001);
	TickField t3 = factory.makeTickField("rb2210", 5002);
	TickField t4 = factory.makeTickField("rb2210", 5000);
	
	BarField bar = factory.makeBarField("rb2210", 5000, 20, ldt); 
	
	PlaybackGatewayFactory playbackGatewayFactory;
	Contract c = mock(Contract.class);
	
	@BeforeEach
	void prepare() {
		when(contractMgr.getContract(any(), anyString())).thenReturn(c);
		when(c.contractField()).thenReturn(contract);
		
		
		settings.setStartDate("20220629");
		settings.setEndDate("20220629");
		settings.setPrecision(PlaybackPrecision.LOW);
		settings.setSpeed(PlaybackSpeed.SPRINT);
		settings.setPlayContracts(List.of());
		
		playbackGatewayFactory = new PlaybackGatewayFactory(feEngine, contractMgr, rtRepo, dsMgr);
	}
	
	@Test
	void testRunning() throws InterruptedException {
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("testGateway")
				.settings(settings)
				.build();
		
		assertDoesNotThrow(() -> {
			playbackGatewayFactory.newInstance(gd);
		});
		
	}

}
