package org.dromara.northstar.gateway.playback.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.MarketDataLoadingUtils;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.playback.ticker.RandomWalkTickSimulation;
import org.dromara.northstar.gateway.playback.ticker.SimpleCloseSimulation;
import org.dromara.northstar.gateway.playback.ticker.SimplePriceSimulation;
import org.dromara.northstar.gateway.playback.ticker.TickSimulationAlgorithm;

import com.google.common.collect.Lists;

import lombok.Getter;

public class ContractDataLoader {
	
	private IDataSource dsMgr;
	@Getter
	private IContract contract;
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	private Queue<Bar> barQ = new LinkedList<>();
	
	private Queue<Tick> tickQ = new LinkedList<>();
	
	private String gatewayId;
	
	private TickSimulationAlgorithm tickGenAlgo;
	
	public ContractDataLoader(String gatewayId, IContract contract, PlaybackPrecision precision) {
		this.contract = contract;
		this.dsMgr = contract.dataSource();
		this.gatewayId = gatewayId;
		Contract cf = contract.contract();
		this.tickGenAlgo = switch(precision) {
		case LITE -> new SimpleCloseSimulation(cf.priceTick());
		case LOW -> new SimplePriceSimulation(cf.priceTick());
		case MEDIUM -> new RandomWalkTickSimulation(30, cf.priceTick());
		case HIGH -> new RandomWalkTickSimulation(120, cf.priceTick());
		default -> throw new IllegalArgumentException("Unexpected value: " + precision);
		};
	}
	
	public void loadBars(LocalDate tradingDay) {
		barQ.clear();
		LocalDate thisFriday = utils.getFridayOfThisWeek(tradingDay);
		LocalDate queryFrom = tradingDay.isAfter(thisFriday) ? thisFriday : thisFriday.minusWeeks(1);
		LocalDate queryTo = tradingDay.isAfter(thisFriday) ? thisFriday.plusWeeks(1) : thisFriday;
		List<Bar> dailyData = Lists.reverse(dsMgr.getMinutelyData(contract.contract(), queryFrom, queryTo)); // 原数据是倒序的
		if(dailyData.isEmpty()) {
			return;
		}
		for(int i=0; i < dailyData.size() ; i++) {
			double openInterestDelta = 0;
			if(i > 0) {
				openInterestDelta = dailyData.get(i).openInterest() - dailyData.get(i - 1).openInterest();
			}
			if(Objects.equals(dailyData.get(i).tradingDay(), tradingDay)) {
				Bar srcBar = dailyData.get(i);
				barQ.add(srcBar.toBuilder()
						.gatewayId(gatewayId)
						.openInterestDelta(openInterestDelta)
						.build());
			}
		}
	}
	
	public boolean hasMoreBar() {
		return !barQ.isEmpty();
	}
	
	public Bar nextBar(boolean remove) {
		return remove ? barQ.poll() : barQ.peek();
	}
	
	public void loadBarsAndTicks(LocalDate tradingDay){
		loadBars(tradingDay);
		tickQ.clear();
		LocalDate thisFriday = utils.getFridayOfThisWeek(tradingDay);
		LocalDate queryFrom = tradingDay.isAfter(thisFriday) ? thisFriday : thisFriday.minusWeeks(1);
		LocalDate queryTo = tradingDay.isAfter(thisFriday) ? thisFriday.plusWeeks(1) : thisFriday;
		List<Bar> dailyData = Lists.reverse(dsMgr.getDailyData(contract.contract(), queryFrom, queryTo));
		for(int i=1; i<dailyData.size(); i++) {
			Bar dayBar = dailyData.get(dailyData.size() - i);
			if(Objects.equals(tradingDay, dayBar.tradingDay())) {
				AtomicLong accVol = new AtomicLong();
				barQ.forEach(bar -> 
					tickGenAlgo.generateFrom(bar).forEach(tickEntry -> {
						LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(tickEntry.timestamp()), ZoneId.systemDefault());
						Tick tick = Tick.builder()
								.preClosePrice(dayBar.preClosePrice())
								.preOpenInterest(dayBar.preOpenInterest())
								.preSettlePrice(dayBar.preSettlePrice())
								.lowerLimit(dayBar.preSettlePrice() * (1 - 0.07))
								.upperLimit(dayBar.preSettlePrice() * (1 + 0.07))
								.highPrice(dayBar.highPrice())				// 采用K线未来值
								.lowPrice(dayBar.lowPrice())				// 采用K线未来值
								.contract(dayBar.contract())
								.tradingDay(bar.tradingDay())
								.actionDay(bar.actionDay())
								.actionTime(ldt.toLocalTime())
								.actionTimestamp(tickEntry.timestamp())
								.type(TickType.MARKET_TICK)
								.lastPrice(tickEntry.price())
								.askPrice(List.of(tickEntry.askPrice0())) // 仅模拟卖一价
								.bidPrice(List.of(tickEntry.bidPrice0())) // 仅模拟买一价
								.askVolume(List.of(ThreadLocalRandom.current().nextInt(10,500))) // 随机模拟卖一量
								.bidVolume(List.of(ThreadLocalRandom.current().nextInt(10,500))) // 随机模拟买一量
								.gatewayId(gatewayId)
								.channelType(ChannelType.PLAYBACK)
								.volume(accVol.addAndGet(Math.max(1, tickEntry.volume())))
								.volumeDelta(Math.max(1, tickEntry.volume()))		// 采用模拟随机值
								.openInterest(bar.openInterest())					// 采用分钟K线的模糊值
								.openInterestDelta(tickEntry.openInterestDelta())	// 采用模拟随机值
								.turnoverDelta(bar.turnoverDelta())					// 采用分钟K线的模糊值
								.turnover(bar.turnover())							// 采用分钟K线的模糊值
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
	
	public Tick nextTick(boolean remove) {
		return remove ? tickQ.poll() : tickQ.peek();
	}
}
