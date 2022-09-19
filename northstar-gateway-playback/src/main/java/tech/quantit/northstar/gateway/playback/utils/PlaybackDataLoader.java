package tech.quantit.northstar.gateway.playback.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import tech.quantit.northstar.common.utils.MarketDataLoadingUtils;
import tech.quantit.northstar.data.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public class PlaybackDataLoader {

	private IMarketDataRepository mdRepo;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	public PlaybackDataLoader(IMarketDataRepository mdRepo) {
		this.mdRepo = mdRepo;
	}
	
	public List<BarField> loadData(LocalDateTime fromStartDateTime, ContractField contract){
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
		return mdRepo.loadBars("CTP", contract.getUnifiedSymbol(), queryStart, queryEnd)
				.stream()
				.filter(bar -> bar.getActionTimestamp() >= fromStartTimestamp)
				.toList();
	}
	
	public List<BarField> loadDataRaw(LocalDate startDate, LocalDate endDate, ContractField contract){
		return mdRepo.loadBars("CTP", contract.getUnifiedSymbol(), startDate, endDate);
	}
}
