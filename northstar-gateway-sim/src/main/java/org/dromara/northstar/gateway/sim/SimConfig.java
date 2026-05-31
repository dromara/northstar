package org.dromara.northstar.gateway.sim;

import java.util.Map;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.sim.market.SimTickGenerator;
import org.dromara.northstar.gateway.sim.trade.SimContractGenerator;
import org.dromara.northstar.gateway.sim.trade.SimGatewayFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SimConfig{
	
	@Bean
	Map<String, SimTickGenerator> tickGeneratorMap(MarketCenter marketCenter, SimContractDefProvider contractDefPvd){
		// 加载模拟合约
		SimContractGenerator contractGen = new SimContractGenerator("SIM");
		marketCenter.addInstrument(contractGen.getContract());
		marketCenter.addInstrument(contractGen.getContract2());
		marketCenter.addInstrument(contractGen.getContract3());
		log.debug("加载模拟合约");
		return marketCenter.getContracts(ChannelType.SIM).stream()
				.map(c -> new SimTickGenerator(c.contract()))
				.collect(Collectors.toMap(c -> c.tickSymbol(), c -> c));
	}

	@Bean
	SimGatewayFactory simGatewayFactory(FastEventEngine feEngine, IAccountRepository accountRepo,
			MarketCenter marketCenter, Map<String, SimTickGenerator> tickGenMap) {
		return new SimGatewayFactory(feEngine, accountRepo, marketCenter, tickGenMap);
	}
	
}
