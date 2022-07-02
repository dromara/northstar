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
		long fromStartTimestamp = fromStartDateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		LocalDate endOfLastWeek = utils.getFridayOfLastWeek(fromStartTimestamp);
		LocalDate endOfThisWeek = utils.getFridayOfThisWeek(fromStartDateTime.toLocalDate());
		return mdRepo.loadBars("CTP", contract.getUnifiedSymbol(), endOfLastWeek, endOfThisWeek)
				.stream()
				.filter(bar -> bar.getActionTimestamp() >= fromStartTimestamp)
				.toList();
	}
}
