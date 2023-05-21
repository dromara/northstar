package org.dromara.northstar.gateway.ctp;

import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.IMarketCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnExpression("!'${spring.profiles.active}'.equals('test')")
public class CtpLoader implements CommandLineRunner{

	@Autowired
	private IMarketCenter mktCenter;
	
	@Autowired
	private CtpDataServiceManager dsMgr;
	
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
	}

}
