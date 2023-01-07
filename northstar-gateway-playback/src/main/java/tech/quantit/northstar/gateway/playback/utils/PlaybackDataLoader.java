package tech.quantit.northstar.gateway.playback.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import tech.quantit.northstar.common.constant.ChannelType;
import tech.quantit.northstar.common.utils.MarketDataLoadingUtils;
import tech.quantit.northstar.data.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public class PlaybackDataLoader {

	private IMarketDataRepository mdRepo;
	
	private String gatewayId;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	public PlaybackDataLoader(String playbackGatewayId, IMarketDataRepository mdRepo) {
		this.mdRepo = mdRepo;
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
		return enhanceData(mdRepo.loadBars(ChannelType.CTP, contract.getUnifiedSymbol(), queryStart, queryEnd)
				.stream()
				.filter(bar -> bar.getActionTimestamp() >= fromStartTimestamp)
				.toList(), contract.getUnifiedSymbol());
	}
	
	public List<BarField> loadMinuteDataRaw(LocalDate startDate, LocalDate endDate, ContractField contract){
		return enhanceData(mdRepo.loadBars(ChannelType.CTP, contract.getUnifiedSymbol(), startDate, endDate), contract.getUnifiedSymbol());
	}
	
	public List<BarField> loadTradeDayDataRaw(LocalDate startDate, LocalDate endDate, ContractField contract){
		return enhanceData(mdRepo.loadDailyBars("CTP", contract.getUnifiedSymbol(), startDate, endDate), contract.getUnifiedSymbol());
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
					.setOpenInterestDelta(openInterestDelta)
					.build());
		}
		return results;
	}
}
