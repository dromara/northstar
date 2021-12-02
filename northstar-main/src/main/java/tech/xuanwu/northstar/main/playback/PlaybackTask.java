package tech.xuanwu.northstar.main.playback;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import tech.xuanwu.northstar.common.constant.DateTimeConstant;
import tech.xuanwu.northstar.common.constant.PlaybackPrecision;
import tech.xuanwu.northstar.common.model.PlaybackDescription;
import tech.xuanwu.northstar.domain.strategy.StrategyModule;
import tech.xuanwu.northstar.main.persistence.MarketDataRepository;
import tech.xuanwu.northstar.main.persistence.po.MinBarDataPO;
import tech.xuanwu.northstar.main.persistence.po.TickDataPO;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 负责提供回测数据
 * @author KevinHuangwl
 *
 */
public class PlaybackTask {
	
	private PlaybackPrecision precision;
	
	private List<StrategyModule> playbackModules;
	
	protected long totalNumOfDays;
	
	protected long totalNumOfData;
	
	protected LocalDate curDate;
	
	protected LocalDate endDate;
	
	protected PriorityQueue<TickField> tickQ;
	
	protected PriorityQueue<BarField> barQ;
	
	private MarketDataRepository mdRepo;

	public PlaybackTask(PlaybackDescription playbackDescription, List<StrategyModule> playbackModules, MarketDataRepository mdRepo) {
		this.playbackModules = playbackModules;
		this.precision = playbackDescription.getPrecision();
		this.mdRepo = mdRepo;
		this.curDate = LocalDate.parse(playbackDescription.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		this.endDate = LocalDate.parse(playbackDescription.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		this.totalNumOfDays = restOfDays();
	}
	
	public Map<DataType, PriorityQueue<?>> nextBatchData(){
		if(isDone()) {
			throw new IllegalStateException("超出回测范围");
		}
		Map<DataType, PriorityQueue<?>> resultMap = new EnumMap<>(DataType.class);
		tickQ = new PriorityQueue<>(100000, (t1, t2) -> t1.getActionTimestamp() < t2.getActionTimestamp() ? -1 : 1 );
		barQ = new PriorityQueue<>(3000, (b1, b2) -> b1.getActionTimestamp() < b2.getActionTimestamp() ? -1 : 1 );
		// 先把高维的TICK与BAR数据转成一维
		for(StrategyModule module : playbackModules) {
			Set<String> interestContracts = module.bindedContractUnifiedSymbols();
			for(String unifiedSymbol : interestContracts) {				
				List<MinBarDataPO> data = mdRepo.loadDataByDate(module.getBindedMktGatewayId(), unifiedSymbol, curDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
				for(MinBarDataPO po : data) {
					if(precision == PlaybackPrecision.TICK) {						
						for(TickDataPO tickData : po.getTicksOfMin()) {
							tickQ.offer(restorePlaybackTick(po, tickData));
						}
					} else if (precision == PlaybackPrecision.BAR) {
						// 在回测模式为BAR模式时，TICK数据仅选取每分钟最后一个TICK，以免确保模拟计算时更准确
						List<TickDataPO> list = po.getTicksOfMin();
						tickQ.offer(restorePlaybackTick(po, list.get(list.size() - 1)));
					}
					barQ.offer(restorePlaybackBar(po));
				}
			}
		}
		totalNumOfData = barQ.size();
		resultMap.put(DataType.BAR, barQ);
		resultMap.put(DataType.TICK, tickQ);
		curDate = curDate.plusDays(1);
		return resultMap;
	}
	
	private long restOfDays() {
		return curDate.until(endDate.plusDays(1), ChronoUnit.DAYS);
	}
	
	public boolean isDone() {
		return curDate.isAfter(endDate);
	}
	
	private double ratioOfDay() {
		if(totalNumOfData == 0 || barQ == null || barQ.isEmpty()) {
			return totalNumOfData;
		}
		return 1.0 * (totalNumOfData - barQ.size()) / totalNumOfData; 
	}
	
	public double ratioOfProcess() {
		return 1.0 * (totalNumOfDays - restOfDays()) / totalNumOfDays + ratioOfDay() / totalNumOfDays;
	}
	
	private BarField restorePlaybackBar(MinBarDataPO barData) {
		return BarField.newBuilder()
				.setActionDay(barData.getActionDay())
				.setActionTime(barData.getActionTime())
				.setActionTimestamp(barData.getActionTimestamp())
				.setTradingDay(barData.getTradingDay())
				.setGatewayId(barData.getGatewayId())
				.setUnifiedSymbol(barData.getUnifiedSymbol())
				.setHighPrice(barData.getHighPrice())
				.setLowPrice(barData.getLowPrice())
				.setOpenPrice(barData.getOpenPrice())
				.setClosePrice(barData.getClosePrice())
				.setPreClosePrice(barData.getPreClosePrice())
				.setPreOpenInterest(barData.getPreOpenInterest())
				.setPreSettlePrice(barData.getPreSettlePrice())
				.setVolume(barData.getVolume())
				.setVolumeDelta(barData.getVolumeDelta())
				.setOpenInterest(barData.getOpenInterest())
				.setOpenInterestDelta(barData.getOpenInterestDelta())
				.setNumTrades(barData.getNumTrades())
				.setNumTradesDelta(barData.getNumTradesDelta())
				.setTurnover(barData.getTurnover())
				.setTurnoverDelta(barData.getTurnoverDelta())
				.build();
	}
	
	private TickField restorePlaybackTick(MinBarDataPO barData, TickDataPO tickData) {
		return TickField.newBuilder()
				.setActionDay(barData.getActionDay())
				.setActionTime(tickData.getActionTime())
				.setActionTimestamp(tickData.getActionTimestamp())
				.setTradingDay(barData.getTradingDay())
				.addAskPrice(tickData.getAskPrice1())
				.addBidPrice(tickData.getBidPrice1())
				.setGatewayId(barData.getGatewayId())
				.setLastPrice(tickData.getLastPrice())
				.setAvgPrice(tickData.getAvgPrice())
				.setUnifiedSymbol(barData.getUnifiedSymbol())
				.setTurnover(tickData.getTurnover())
				.setTurnoverDelta(tickData.getTurnoverDelta())
				.addAskVolume(tickData.getAskVol1())
				.addBidVolume(tickData.getBidVol1())
				.setVolume(tickData.getVolume())
				.setVolumeDelta(tickData.getVolumeDelta())
				.setNumTrades(tickData.getNumTrades())
				.setNumTradesDelta(tickData.getNumTradesDelta())
				.setOpenInterest(tickData.getOpenInterest())
				.setOpenInterestDelta(tickData.getOpenInterestDelta())
				.setPreClosePrice(barData.getPreClosePrice())
				.setPreOpenInterest(barData.getPreOpenInterest())
				.setPreSettlePrice(barData.getPreSettlePrice())
				.build();
	}
	
	public enum DataType {
		BAR,
		TICK
	}
}
