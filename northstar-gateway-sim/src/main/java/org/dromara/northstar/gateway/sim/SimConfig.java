package org.dromara.northstar.gateway.sim;

import java.util.Map;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.data.ISimAccountRepository;
import org.dromara.northstar.gateway.IMarketCenter;
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
	Map<String, SimTickGenerator> tickGeneratorMap(IMarketCenter marketCenter, SimContractDefProvider contractDefPvd){
		// 加载模拟合约
		SimContractGenerator contractGen = new SimContractGenerator("SIM");
		marketCenter.addDefinitions(contractDefPvd.get());
		marketCenter.addInstrument(contractGen.getContract());
		marketCenter.addInstrument(contractGen.getContract2());
		log.debug("加载模拟合约");
		return marketCenter.getContracts(ChannelType.SIM).stream()
				.map(c -> new SimTickGenerator(c.contractField()))
				.collect(Collectors.toMap(c -> c.contract().getUnifiedSymbol(), c -> c));
	}

	@Bean
	SimGatewayFactory simGatewayFactory(FastEventEngine feEngine, ISimAccountRepository simAccountRepo, IMarketCenter marketCenter, 
			Map<String, SimTickGenerator> tickGenMap) {
		return new SimGatewayFactory(feEngine, simAccountRepo, marketCenter, tickGenMap);
	}
	
	@Bean
	SimMockDataManager simMockDataManager(Map<String, SimTickGenerator> tickGenMap) {
		return new SimMockDataManager(tickGenMap);
	}
	
}
