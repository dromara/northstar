package tech.quantit.northstar.gateway.api.utils;

import java.util.Map;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.model.GatewayDescription;
import tech.quantit.northstar.data.IGatewayRepository;
import tech.quantit.northstar.gateway.api.IMarketDataRepository;

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
