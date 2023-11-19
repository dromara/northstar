package org.dromara.northstar.gateway.playback.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.utils.MarketDataLoadingUtils;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.playback.ticker.RandomWalkTickSimulation;
import org.dromara.northstar.gateway.playback.ticker.SimpleCloseSimulation;
import org.dromara.northstar.gateway.playback.ticker.SimplePriceSimulation;
import org.dromara.northstar.gateway.playback.ticker.TickSimulationAlgorithm;

import com.google.common.collect.Lists;

import lombok.Getter;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.TickField;

public class ContractDataLoader {
	
	private IDataSource dsMgr;
	@Getter
	private Contract contract;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	private Queue<BarField> barQ = new LinkedList<>();
	
	private Queue<TickField> tickQ = new LinkedList<>();
	
	private String gatewayId;
	
	private TickSimulationAlgorithm tickGenAlgo;
	
	public ContractDataLoader(String gatewayId, Contract contract, PlaybackPrecision precision) {
		this.contract = contract;
		this.dsMgr = contract.dataSource();
		this.gatewayId = gatewayId;
		ContractField cf = contract.contractField();
		this.tickGenAlgo = switch(precision) {
		case LITE -> new SimpleCloseSimulation(cf.getPriceTick());
		case LOW -> new SimplePriceSimulation(cf.getPriceTick());
		case MEDIUM -> new RandomWalkTickSimulation(30, cf.getPriceTick());
		case HIGH -> new RandomWalkTickSimulation(120, cf.getPriceTick());
		default -> throw new IllegalArgumentException("Unexpected value: " + precision);
		};
	}
	
	public void loadBars(LocalDate tradingDay) {
		barQ.clear();
		LocalDate thisFriday = utils.getFridayOfThisWeek(tradingDay);
		LocalDate lastFriday = thisFriday.minusWeeks(1);
		List<BarField> dailyData = Lists.reverse(dsMgr.getMinutelyData(contract.contractField(), lastFriday, thisFriday)); // 原数据是倒序的
		if(dailyData.isEmpty()) {
			return;
		}
		for(int i=0; i < dailyData.size() ; i++) {
			double openInterestDelta = 0;
			if(i > 0) {
				openInterestDelta = dailyData.get(i).getOpenInterest() - dailyData.get(i - 1).getOpenInterest();
			}
			if(dailyData.get(i).getTradingDay().equals(tradingDay.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))) {
				barQ.add(dailyData.get(i).toBuilder()
						.setGatewayId(gatewayId)
						.setOpenInterestDelta(openInterestDelta)
						.build());
			}
		}
	}
	
	public boolean hasMoreBar() {
		return !barQ.isEmpty();
	}
	
	public BarField nextBar(boolean remove) {
		return remove ? barQ.poll() : barQ.peek();
	}
	
	public void loadBarsAndTicks(LocalDate tradingDay){
		loadBars(tradingDay);
		tickQ.clear();
		LocalDate end = utils.getFridayOfThisWeek(tradingDay);
		LocalDate start = end.minusWeeks(2);
		List<BarField> dailyData = Lists.reverse(dsMgr.getDailyData(contract.contractField(), start, end));
		for(int i=1; i<dailyData.size(); i++) {
			BarField dayBar = dailyData.get(dailyData.size() - i);
			if(StringUtils.equals(dayBar.getTradingDay(), tradingDay.format(DateTimeConstant.D_FORMAT_INT_FORMATTER))) {
				AtomicLong accVol = new AtomicLong();
				barQ.forEach(bar -> 
					tickGenAlgo.generateFrom(bar).forEach(tickEntry -> {
						TickField tick = TickField.newBuilder()
								.setPreClosePrice(dayBar.getPreClosePrice())
								.setPreOpenInterest(dayBar.getPreOpenInterest())
								.setPreSettlePrice(dayBar.getPreSettlePrice())
								.setLowerLimit(dayBar.getPreSettlePrice() * (1 - 0.07))	// 采用7%跌停幅度
								.setUpperLimit(dayBar.getPreSettlePrice() * (1 + 0.07))	// 采用7%涨停幅度
								.setHighPrice(dayBar.getHighPrice())			// 采用K线未来值
								.setLowPrice(dayBar.getLowPrice())				// 采用K线未来值
								.setUnifiedSymbol(dayBar.getUnifiedSymbol())
								.setTradingDay(bar.getTradingDay())
								.setStatus(TickType.NORMAL_TICK.getCode())
								.setActionDay(bar.getActionDay())
								.setActionTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(tickEntry.timestamp()), ZoneId.systemDefault()).format(DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER))
								.setActionTimestamp(tickEntry.timestamp())
								.setLastPrice(tickEntry.price())
								.addAllAskPrice(List.of(tickEntry.askPrice0(), 0D, 0D, 0D, 0D)) // 仅模拟卖一价
								.addAllBidPrice(List.of(tickEntry.bidPrice0(), 0D, 0D, 0D, 0D)) // 仅模拟买一价
								.addAllAskVolume(List.of(ThreadLocalRandom.current().nextInt(10,500))) // 随机模拟卖一量
								.addAllBidVolume(List.of(ThreadLocalRandom.current().nextInt(10,500))) // 随机模拟买一量
								.setGatewayId(gatewayId)
								.setChannelType(ChannelType.PLAYBACK.toString())
								.setVolume(accVol.addAndGet(Math.max(1, tickEntry.volume())))
								.setVolumeDelta(Math.max(1, tickEntry.volume()))			// 采用模拟随机值
								.setOpenInterest(bar.getOpenInterest())			// 采用分钟K线的模糊值
								.setOpenInterestDelta(tickEntry.openInterestDelta())		// 采用模拟随机值
								.setTurnoverDelta(bar.getTurnoverDelta())		// 采用分钟K线的模糊值
								.setTurnover(bar.getTurnover())					// 采用分钟K线的模糊值
								.build();
						tickQ.add(tick);
					})
				);
			}
		}
	}
	
	public boolean hasMoreTick() {
		return !tickQ.isEmpty();
	}
	
	public TickField nextTick(boolean remove) {
		return remove ? tickQ.poll() : tickQ.peek();
	}
}
