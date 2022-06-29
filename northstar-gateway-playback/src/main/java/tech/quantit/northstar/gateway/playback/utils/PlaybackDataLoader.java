package tech.quantit.northstar.gateway.playback.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.utils.MarketDataLoadingUtils;
import tech.quantit.northstar.data.IMarketDataRepository;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;

public class PlaybackDataLoader {

	private IMarketDataRepository mdRepo;
	
	private IContractManager contractMgr;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	public PlaybackDataLoader(IContractManager contractMgr, IMarketDataRepository mdRepo) {
		this.contractMgr = contractMgr;
		this.mdRepo = mdRepo;
	}
	
	public Map<ContractField, List<BarField>> loadData(long fromStartTimestamp, String contractGroup){
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(fromStartTimestamp), ZoneId.systemDefault());
		LocalDate endOfLastWeek = utils.getFridayOfLastWeek(fromStartTimestamp);
		LocalDate endOfThisWeek = utils.getFridayOfThisWeek(ldt.toLocalDate());
		return contractMgr.relativeContracts(contractGroup)
				.stream()
				.collect(Collectors.toMap(contract -> contract, 
						contract -> mdRepo.loadBars("CTP", contract.getUnifiedSymbol(), endOfLastWeek, endOfThisWeek)
							.stream()
							.filter(bar -> bar.getActionTimestamp() >= fromStartTimestamp)
							.toList()));
	}
}
