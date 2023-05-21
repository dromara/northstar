package org.dromara.northstar.gateway.sim;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.Instrument;
import org.dromara.northstar.gateway.sim.trade.SimContractGenerator;
import org.dromara.northstar.gateway.sim.trade.SimGatewayFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(0)
@Component
public class SimLoader implements CommandLineRunner{

	@Autowired
	private IMarketCenter mktCenter;
	
	@Autowired
	private GatewayMetaProvider gatewayMetaProvider;
	
	@Autowired
	private SimGatewayFactory simGatewayFactory;
	
	@Autowired
	private DefaultEmptyMarketDataRepo defaultEmptyRepo;
	
	@Override
	public void run(String... args) throws Exception {
		gatewayMetaProvider.add(ChannelType.SIM, null, simGatewayFactory, defaultEmptyRepo);
		
		log.debug("加载模拟合约");
		// 加载模拟合约
		SimContractGenerator contractGen = new SimContractGenerator("SIM");
		Instrument simContract = contractGen.getContract();
		mktCenter.addInstrument(simContract);
	}

}
