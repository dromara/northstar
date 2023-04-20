package org.dromara.northstar.gateway.common.utils;

import java.util.Map;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.data.IGatewayRepository;
import org.dromara.northstar.data.IMarketDataRepository;

public class MarketDataRepoFactory {
	
	private final Map<ChannelType, IMarketDataRepository> channelRepoMap;
	
	private final IGatewayRepository gatewayRepo;
	
	public MarketDataRepoFactory(Map<ChannelType, IMarketDataRepository> channelRepoMap, IGatewayRepository gatewayRepo) {
		this.channelRepoMap = channelRepoMap;
		this.gatewayRepo = gatewayRepo;
	}

	public IMarketDataRepository getInstance(ChannelType channelType) {
		if(channelRepoMap.containsKey(channelType)) {
			return channelRepoMap.get(channelType);
		}
		throw new IllegalStateException("没有该渠道的数据来源：" + channelType);
	}
	
	public IMarketDataRepository getInstance(String gatewayId) {
		GatewayDescription gd = gatewayRepo.findById(gatewayId);
		if(gd == null) {
			throw new IllegalStateException("没有该网关的数据来源：" + gatewayId);
		}
		return getInstance(gd.getChannelType());
	}
}
