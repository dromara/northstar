package org.dromara.northstar.main;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.data.ds.DataServiceManager;
import org.dromara.northstar.data.ds.W3DataServiceManager;
import org.dromara.northstar.gateway.api.IMarketCenter;
import org.dromara.northstar.gateway.api.domain.contract.Instrument;
import org.dromara.northstar.gateway.ctp.CtpContract;
import org.dromara.northstar.gateway.okx.OkxContract;
import org.dromara.northstar.gateway.sim.trade.SimContractGenerator;
import org.dromara.northstar.main.service.GatewayService;
import org.dromara.northstar.main.service.ModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum;

@Slf4j
@Component
@ConditionalOnExpression("!'${spring.profiles.active}'.equals('test')")
public class ContractsDefaultLoader implements CommandLineRunner{

	@Autowired
	private IMarketCenter mktCenter;
	
	@Autowired
	private DataServiceManager dsMgr;
//	@Autowired
//	private W3DataServiceManager w3dsMgr;
	@Autowired
	private GatewayService gatewayService;
	
	@Autowired
	private ModuleService moduleService;
	
	@Override
	public void run(String... args) throws Exception {
		final LocalDate today = LocalDate.now();
		// 加载CTP合约
		dsMgr.getUserAvailableExchanges()
			.parallelStream()
			.forEach(exchange -> {
				dsMgr.getAllContracts(exchange).stream()
					//过滤掉过期合约
					.filter(contract -> StringUtils.isEmpty(contract.getLastTradeDateOrContractMonth())
							|| LocalDate.parse(contract.getLastTradeDateOrContractMonth(), DateTimeConstant.D_FORMAT_INT_FORMATTER).isAfter(today))
					.forEach(contract -> mktCenter.addInstrument(new CtpContract(contract)));
				log.info("预加载 [{}] 交易所合约信息", exchange);
			});
		mktCenter.loadContractGroup(ChannelType.CTP);
		// 加载币圈OKX市场合约
//		List.of(CoreEnum.ExchangeEnum.OKX)
//				.parallelStream()
//				.forEach(exchange -> {
//					w3dsMgr.getAllContracts(exchange)
//							.forEach(contract -> mktCenter.addInstrument(new OkxContract(contract)));
//					log.info("预加载 [{}] w3交易所合约信息", exchange);
//				});

		// 加载模拟合约
		SimContractGenerator contractGen = new SimContractGenerator("SIM");
		Instrument simContract = contractGen.getContract();
		mktCenter.addInstrument(simContract);
		
		gatewayService.postLoad();
		moduleService.postLoad();
	}

}
