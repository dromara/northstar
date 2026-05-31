package org.dromara.northstar.gateway.sim.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.GatewayUsage;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.gateway.Gateway;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.sim.market.SimMarketGateway;
import org.dromara.northstar.gateway.sim.market.SimTickGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.HashBasedTable;

class SimGatewayFactoryTest {

	private SimGatewayFactory factory;
	private FastEventEngine feEngine;
	private IAccountRepository accountRepo;
	private MarketCenter mktCenter;
	private Map<String, SimTickGenerator> tickGenMap;

	@BeforeEach
	void setUp() {
		feEngine = mock(FastEventEngine.class);
		accountRepo = mock(IAccountRepository.class);
		mktCenter = mock(MarketCenter.class);
		tickGenMap = new HashMap<>();
		
		factory = new SimGatewayFactory(feEngine, accountRepo, mktCenter, tickGenMap);
	}

	@Test
	void testNewMarketDataGatewayInstance() {
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("TEST_MKT")
				.gatewayUsage(GatewayUsage.MARKET_DATA)
				.build();
		
		Gateway gateway = factory.newInstance(gd);
		
		assertThat(gateway).isInstanceOf(SimMarketGateway.class);
		assertThat(gateway.getDescription().getGatewayId()).isEqualTo("TEST_MKT");
	}

	@Test
	void testNewTradeGatewayInstance() {
		when(accountRepo.findAccountsAsAggregateAccount(anyString())).thenReturn(
				org.dromara.northstar.common.model.core.Account.builder()
						.deposit(1000000)
						.available(1000000)
						.build());
		when(accountRepo.findHoldingTrades(anyString())).thenReturn(HashBasedTable.create());
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("TEST_TRADE")
				.gatewayUsage(GatewayUsage.TRADE)
				.build();
		
		Gateway gateway = factory.newInstance(gd);
		
		assertThat(gateway).isInstanceOf(SimTradeGateway.class);
		assertThat(gateway.getDescription().getGatewayId()).isEqualTo("TEST_TRADE");
	}

	@Test
	void testChannelType() {
		assertThat(factory.channelType()).isEqualTo(ChannelType.SIM);
	}

	@Test
	void testEnhanceDoesNotModify() {
		GatewayDescription gd = GatewayDescription.builder()
				.gatewayId("TEST")
				.build();
		
		GatewayDescription enhanced = factory.enhance(gd);
		
		assertThat(enhanced).isEqualTo(gd);
	}

}
