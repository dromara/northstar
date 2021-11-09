package tech.xuanwu.northstar.main.playback;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import tech.xuanwu.northstar.common.constant.PlaybackPrecision;
import tech.xuanwu.northstar.common.model.PlaybackDescription;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.strategy.common.StrategyModule;

/**
 * 负责提供回测批数据
 * @author KevinHuangwl
 *
 */
public class PlaybackTask {
	
	private PlaybackPrecision precision;
	
	private List<StrategyModule> playbackModules;
	
	private long totalNumOfDays;
	
	private LocalDate curDate;
	
	private LocalDate endDate;
	
	private MarketDataRepository mdRepo;

	public PlaybackTask(PlaybackDescription playbackDescription, List<StrategyModule> playbackModules, MarketDataRepository mdRepo) {
		this.playbackModules = playbackModules;
		this.precision = playbackDescription.getPrecision();
		this.mdRepo = mdRepo;
		this.curDate = LocalDate.parse(playbackDescription.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		this.endDate = LocalDate.parse(playbackDescription.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		this.totalNumOfDays = restOfDays();
	}
	
	public List<MinBarDataPO> nextBatchData(){
		if(isDone()) {
			throw new IllegalStateException("超出回测范围");
		}
		List<MinBarDataPO> result = new LinkedList<>();
		for(StrategyModule module : playbackModules) {
			Set<String> interestContracts = module.getInterestContractUnifiedSymbol();
			for(String unifiedSymbol : interestContracts) {				
				List<MinBarDataPO> data = mdRepo.loadDataByDate(module.getBindedMarketGatewayId(), unifiedSymbol, curDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
				result.addAll(data);
			}
		}
		curDate = curDate.plusDays(1);
		return result;
	}
	
	private long restOfDays() {
		return curDate.until(endDate.plusDays(1), ChronoUnit.DAYS);
	}
	
	public boolean isDone() {
		return curDate.isAfter(endDate);
	}
	
	public PlaybackPrecision getPrecision() {
		return precision;
	}
	
	public double ratioOfProcess() {
		return 1.0 * (totalNumOfDays - restOfDays()) / totalNumOfDays;
	}
}
