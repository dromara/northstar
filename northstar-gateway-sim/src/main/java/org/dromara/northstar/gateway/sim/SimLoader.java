package org.dromara.northstar.gateway.sim;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.dromara.northstar.common.IDataServiceManager;
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

import com.alibaba.fastjson2.JSONObject;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

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
	private SimContractDefProvider contractDefPvd;
	
	private IDataServiceManager mockDsMgr = new IDataServiceManager() {
		
		@Override
		public List<ExchangeEnum> getUserAvailableExchanges() { return Collections.emptyList();}
		
		@Override
		public List<BarField> getQuarterlyData(ContractField contract, LocalDate startDate, LocalDate endDate) { return Collections.emptyList();}
		
		@Override
		public List<BarField> getMinutelyData(ContractField contract, LocalDate startDate, LocalDate endDate) { return Collections.emptyList();}
		
		@Override
		public List<BarField> getHourlyData(ContractField contract, LocalDate startDate, LocalDate endDate) { return Collections.emptyList();}
		
		@Override
		public List<LocalDate> getHolidays(ExchangeEnum exchange, LocalDate startDate, LocalDate endDate) { return Collections.emptyList();}
		
		@Override
		public List<BarField> getDailyData(ContractField contract, LocalDate startDate, LocalDate endDate) { return Collections.emptyList();}
		
		@Override
		public JSONObject getCtpMetaSettings(String brokerId) { return new JSONObject(); }
		
		@Override
		public List<ContractField> getAllContracts(ExchangeEnum exchange) { return Collections.emptyList();}
	};
	
	@Override
	public void run(String... args) throws Exception {
		gatewayMetaProvider.add(ChannelType.SIM, null, simGatewayFactory, mockDsMgr);

		mktCenter.addDefinitions(contractDefPvd.get());
		log.debug("加载模拟合约");
		// 加载模拟合约
		SimContractGenerator contractGen = new SimContractGenerator("SIM");
		Instrument simContract = contractGen.getContract();
		mktCenter.addInstrument(simContract);
	}

}
