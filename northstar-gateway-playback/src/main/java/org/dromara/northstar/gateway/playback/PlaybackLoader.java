package org.dromara.northstar.gateway.playback;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.gateway.GatewayMetaProvider;
import org.dromara.northstar.gateway.IMarketCenter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(0)
@Component
public class PlaybackLoader implements CommandLineRunner{
	
	@Autowired
	GatewayMetaProvider gatewayMetaProvider;
	
	@Autowired
	PlaybackGatewayFactory playbackGatewayFactory;
	
	@Autowired
	List<IDataSource> datasources;
	
	@Autowired
	IMarketCenter mktCenter;
	
	@Autowired
	PlaybackContractDefProvider contractDefPvd;
	
	@Override
	public void run(String... args) throws Exception {
		gatewayMetaProvider.add(ChannelType.PLAYBACK, new PlaybackGatewaySettings(), playbackGatewayFactory);
		mktCenter.addDefinitions(contractDefPvd.get());
		
		log.debug("加载回测合约");
		final LocalDate today = LocalDate.now();
		// 加载CTP合约
		datasources.forEach(ds -> 
			ds.getUserAvailableExchanges().forEach(exchange -> {
				ds.getAllContracts(exchange).stream()
					//过滤掉过期合约
					.filter(contract -> StringUtils.isEmpty(contract.getLastTradeDateOrContractMonth())
							|| LocalDate.parse(contract.getLastTradeDateOrContractMonth(), DateTimeConstant.D_FORMAT_INT_FORMATTER).isAfter(today))
					.forEach(contract -> mktCenter.addInstrument(new PlaybackContract(contract, ds)));
				log.info("预加载 [{}] 交易所合约信息", exchange);
			})
		);
			
		mktCenter.loadContractGroup(ChannelType.PLAYBACK);
	}

}
