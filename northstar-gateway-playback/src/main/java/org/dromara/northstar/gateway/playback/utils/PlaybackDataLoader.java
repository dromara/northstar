package org.dromara.northstar.gateway.playback.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.utils.ContractUtils;
import org.dromara.northstar.common.utils.MarketDataLoadingUtils;
import org.dromara.northstar.gateway.common.utils.MarketDataRepoFactory;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public class PlaybackDataLoader {

	private MarketDataRepoFactory mdRepoFactory;
	
	private String gatewayId;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	public PlaybackDataLoader(String playbackGatewayId, MarketDataRepoFactory mdRepoFactory) {
		this.mdRepoFactory = mdRepoFactory;
		this.gatewayId = playbackGatewayId;
	}
	
	public List<BarField> loadMinuteData(LocalDateTime fromStartDateTime, ContractField contract){
		LocalDate queryStart;
		LocalDate queryEnd;
		long fromStartTimestamp = fromStartDateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		if(fromStartDateTime.toLocalDate().getDayOfWeek().getValue() >= 5) {
			queryStart = utils.getFridayOfThisWeek(fromStartDateTime.toLocalDate());
			queryEnd = queryStart.plusWeeks(1);
		} else {
			queryEnd = utils.getFridayOfThisWeek(fromStartDateTime.toLocalDate());
			queryStart = queryEnd.minusWeeks(1);
		}
		ChannelType channel = ContractUtils.channelTypeOf(contract);
		return enhanceData(mdRepoFactory.getInstance(channel).loadBars(contract, queryStart, queryEnd)
				.stream()
				.filter(bar -> bar.getActionTimestamp() >= fromStartTimestamp)
				.toList(), contract.getUnifiedSymbol());
	}
	
	public List<BarField> loadMinuteDataRaw(LocalDate startDate, LocalDate endDate, ContractField contract){
		ChannelType channel = ContractUtils.channelTypeOf(contract);
		return enhanceData(mdRepoFactory.getInstance(channel).loadBars(contract, startDate, endDate), contract.getUnifiedSymbol());
	}
	
	public List<BarField> loadTradeDayDataRaw(LocalDate startDate, LocalDate endDate, ContractField contract){
		ChannelType channel = ContractUtils.channelTypeOf(contract);
		return enhanceData(mdRepoFactory.getInstance(channel).loadDailyBars(contract, startDate, endDate), contract.getUnifiedSymbol());
	}
	
	private List<BarField> enhanceData(List<BarField> list, String unifiedSymbol) {
		List<BarField> results = new ArrayList<>(list.size());
		for(int i=0; i<list.size(); i++) {
			double openInterestDelta = 0;
			if(i > 0) {
				openInterestDelta = list.get(i).getOpenInterest() - list.get(i - 1).getOpenInterest();
			}
			results.add(list.get(i).toBuilder()
					.setGatewayId(gatewayId)
					.setChannelType(ChannelType.PLAYBACK.toString())
					.setOpenInterestDelta(openInterestDelta)
					.build());
		}
		return results;
	}
}
