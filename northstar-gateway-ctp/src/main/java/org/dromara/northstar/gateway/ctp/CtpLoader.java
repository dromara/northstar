package org.dromara.northstar.gateway.ctp;

import java.time.LocalDate;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.gateway.ctp.x64v6v3v15v.CtpGatewayFactory;
import xyz.redtorch.gateway.ctp.x64v6v5v1cpv.CtpSimGatewayFactory;

@Slf4j
@Order(0)	// 加载顺序需要显式声明，否则会最后才被加载，从而导致加载网关与模组时报异常
@Component
public class CtpLoader implements CommandLineRunner{
	
	@Autowired
	private IMarketCenter mktCenter;
	
	@Resource(name = "ctpDataServiceManager")
	private CtpDataServiceManager dsMgr;
	
	@Resource(name = "ctpSimDataServiceManager")
	private CtpSimDataServiceManager simDsMgr;
	
	@Autowired
	private GatewayMetaProvider gatewayMetaProvider;
	
	@Autowired
	private CtpGatewayFactory ctpFactory;
	
	@Autowired
	private CtpSimGatewayFactory ctpSimFactory;
	
	
	@Override
	public void run(String... args) throws Exception {
		gatewayMetaProvider.add(ChannelType.CTP, new CtpGatewaySettings(), ctpFactory, dsMgr);
		gatewayMetaProvider.add(ChannelType.CTP_SIM, new CtpSimGatewaySettings(), ctpSimFactory, simDsMgr);
		
		final LocalDate today = LocalDate.now();
		// 加载CTP合约
		dsMgr.getUserAvailableExchanges()
			.stream()
			.forEach(exchange -> {
				dsMgr.getAllContracts(exchange).stream()
					//过滤掉过期合约
					.filter(contract -> StringUtils.isEmpty(contract.getLastTradeDateOrContractMonth())
							|| LocalDate.parse(contract.getLastTradeDateOrContractMonth(), DateTimeConstant.D_FORMAT_INT_FORMATTER).isAfter(today))
					.forEach(contract -> mktCenter.addInstrument(new CtpContract(contract)));
				log.info("预加载 [{}] 交易所合约信息", exchange);
			});
		mktCenter.loadContractGroup(ChannelType.CTP);
	}

}
