package tech.quantit.northstar.main.playback;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.google.protobuf.InvalidProtocolBufferException;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.PlaybackPrecision;
import tech.quantit.northstar.common.model.PlaybackDescription;
import tech.quantit.northstar.domain.strategy.StrategyModule;
import tech.quantit.northstar.main.persistence.MarketDataRepository;
import tech.quantit.northstar.main.persistence.po.MinBarDataPO;
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
	
	protected PriorityQueue<TickField> tickQ = new PriorityQueue<>(100000, (t1, t2) -> t1.getActionTimestamp() < t2.getActionTimestamp() ? -1 : 1 );
	
	protected PriorityQueue<BarField> barQ = new PriorityQueue<>(3000, (b1, b2) -> b1.getActionTimestamp() < b2.getActionTimestamp() ? -1 : 1 );
	
	private MarketDataRepository mdRepo;

	public PlaybackTask(PlaybackDescription playbackDescription, List<StrategyModule> playbackModules, MarketDataRepository mdRepo) {
		this.playbackModules = playbackModules;
		this.precision = playbackDescription.getPrecision();
		this.mdRepo = mdRepo;
		this.curDate = LocalDate.parse(playbackDescription.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		this.endDate = LocalDate.parse(playbackDescription.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		this.totalNumOfDays = restOfDays();
	}
	
	public Map<DataType, PriorityQueue<?>> nextBatchDataOfDay() throws InvalidProtocolBufferException{
		if(!hasMoreDayToPlay()) {
			throw new IllegalStateException("超出回测范围");
		}
		Map<DataType, PriorityQueue<?>> resultMap = new EnumMap<>(DataType.class);
		// 先把高维的TICK与BAR数据转成一维
		for(StrategyModule module : playbackModules) {
			Set<String> interestContracts = module.bindedContractUnifiedSymbols();
			for(String unifiedSymbol : interestContracts) {				
				List<MinBarDataPO> data = mdRepo.loadDataByDate(module.getBindedMktGatewayId(), unifiedSymbol, curDate.format(DateTimeConstant.D_FORMAT_INT_FORMATTER));
				for(MinBarDataPO po : data) {
					BarField bar = BarField.parseFrom(po.getBarData());
					barQ.offer(bar);
					if(precision == PlaybackPrecision.TICK) {						
						for(byte[] tickData : po.getTicksData()) {
							tickQ.offer(TickField.parseFrom(tickData));
						}
					} else if (precision == PlaybackPrecision.BAR) {
						// 在回测模式为BAR模式时，TICK数据仅选取每分钟最后一个TICK，以免确保模拟计算时更准确
						TickField firstTick = TickField.parseFrom(po.getTicksData().get(0));
						TickField lastTick = TickField.parseFrom(po.getTicksData().get(po.getTicksData().size() - 1));
						tickQ.offer(firstTick);
						tickQ.offer(lastTick);
					}
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
	
	public boolean hasMoreDayToPlay() {
		return !curDate.isAfter(endDate);
	}
	
	public static void main(String[] args) {
		System.out.println(LocalDate.of(2022, 1, 3).until(LocalDate.of(2022, 1, 8), ChronoUnit.DAYS));
	}
	
	private double ratioOfDay() {
		if(totalNumOfData == 0) {
			return 0;
		}
		return 1.0 * (totalNumOfData - barQ.size()) / totalNumOfData; 
	}
	
	public double ratioOfProcess() {
		double restPercentageOfWork = totalNumOfData == 0 ? 1D * restOfDays() / totalNumOfDays : (1D * restOfDays() + 1 - ratioOfDay()) / totalNumOfDays;
		return 1 - restPercentageOfWork;
	}
	
	
	public enum DataType {
		BAR,
		TICK
	}
}
