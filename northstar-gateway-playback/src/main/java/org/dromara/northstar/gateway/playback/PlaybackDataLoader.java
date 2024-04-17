package org.dromara.northstar.gateway.playback;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.PlaybackPrecision;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.playback.model.DataFrame;
import org.dromara.northstar.gateway.playback.ticker.RandomWalkTickSimulation;
import org.dromara.northstar.gateway.playback.ticker.SimpleCloseSimulation;
import org.dromara.northstar.gateway.playback.ticker.SimplePriceSimulation;
import org.dromara.northstar.gateway.playback.ticker.TickSimulationAlgorithm;
import org.dromara.northstar.gateway.utils.DataLoadUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 回测数据加载器
 * 不止要负责加载历史数据，还要把数据分成数据帧供消费方消费
 * 同时封装了Bar转TICK数据的处理
 * @auth KevinHuangwl
 */
@Slf4j
public class PlaybackDataLoader {
	
	private final Collection<IContract> contracts;
	
	private final TickSimulationAlgorithm tickGenAlgo;
	
	private final int numOfTickPerBar;
	
	private final String gatewayId;
	
	private final DataLoadUtil util = new DataLoadUtil();
	
	public PlaybackDataLoader(String gatewayId, Collection<IContract> contracts, PlaybackPrecision precision) {
		this.gatewayId = gatewayId;
		this.contracts = contracts;
		this.numOfTickPerBar = switch(precision) {
			case LITE -> 1;
			case LOW -> 4;
			case MEDIUM -> 30;
			case HIGH -> 120;
			default -> throw new IllegalArgumentException("Unexpected value: " + precision);
		};
		this.tickGenAlgo = switch(precision) {
			case LITE -> new SimpleCloseSimulation();
			case LOW -> new SimplePriceSimulation();
			case MEDIUM, HIGH -> new RandomWalkTickSimulation(numOfTickPerBar);
			default -> throw new IllegalArgumentException("Unexpected value: " + precision);
		};
	}
	
	public CompletableFuture<Void> preload(LocalDate startDate, LocalDate endDate, Consumer<DataFrame<Bar>> onDataCallback) {
		return CompletableFuture.runAsync(() -> 
			// 切分为按周查询
			util.splitByWeek(startDate, endDate, 
					(start, end) -> loadBarDataFrame(start, end)
							.stream()
							.forEach(onDataCallback::accept))
		);
	}
	
	// 把查询到的Bar数据按时间帧切片
	private List<DataFrame<Bar>> loadBarDataFrame(LocalDate startDate, LocalDate endDate){
		LocalDateTime ldt = LocalDateTime.of(startDate.minusDays(3), LocalTime.of(0, 0));	// 要考虑周五夜盘的数据
		LocalDateTime endDateTime = LocalDateTime.of(endDate.plusDays(1), LocalTime.of(0, 0));
		LinkedList<DataFrame<Bar>> dataFrameList = new LinkedList<>();
		while(!ldt.isAfter(endDateTime)) {
			dataFrameList.add(new DataFrame<>(CommonUtils.localDateTimeToMills(ldt)));
			ldt = ldt.plusMinutes(1);
		}
		Map<Long, DataFrame<Bar>> timeDataMap = dataFrameList.stream().collect(Collectors.toMap(DataFrame::getTimestamp, df -> df));
		
		for(IContract ic : contracts) {
			Contract c = ic.contract();
			IDataSource ds = ic.dataSource();
			ds.getMinutelyData(c, startDate, endDate)
				.stream()
				.map(bar -> bar.toBuilder()
						.gatewayId(gatewayId)
						.channelType(ChannelType.PLAYBACK)
						.build())
				.forEach(bar -> timeDataMap.get(bar.actionTimestamp()).add(bar));
		}
		return dataFrameList.stream().filter(df -> !df.isEmpty()).toList();
	}
	
	public CompletableFuture<Void> load(LocalDate startDate, LocalDate endDate, BooleanSupplier interceptedFlagSupplier,
			Consumer<DataFrame<Tick>> onTickDataCallback,
			BiConsumer<DataFrame<Bar>, Boolean> onBarDataCallback) {
		return CompletableFuture.runAsync(() -> 
			// 切分为按周查询
			util.splitByWeek(startDate, endDate, (start, end) -> {
				// 中断信号检测，确保能正常中断
				if(interceptedFlagSupplier.getAsBoolean()) {
					return;
				}
				List<DataFrame<Bar>> dataBars = loadBarDataFrame(start, end);
				for(DataFrame<Bar> df : dataBars) {
					generateTickFrames(df).forEach(onTickDataCallback::accept);
					Bar bar = df.getSample();
					boolean isLastBar = isLastBarOfTradeDay(bar);
					onBarDataCallback.accept(df, isLastBar);
				}
			})
		);
	}
	
	private boolean isLastBarOfTradeDay(Bar bar) {
		LocalTime t = bar.actionTime();
		TradeTimeDefinition ttd = bar.contract().contractDefinition().tradeTimeDef();
		List<TimeSlot> timeSlots = ttd.timeSlots();
		return timeSlots.get(timeSlots.size() - 1).end().equals(t);
	}
	
	private List<DataFrame<Tick>> generateTickFrames(DataFrame<Bar> barFrame){
		Bar sample = barFrame.getSample();
		List<DataFrame<Tick>> dfList = tickGenAlgo.generateFrom(sample)
			.stream()
			.map(e -> new DataFrame<Tick>(e.timestamp()))
			.toList();
		Map<Long, DataFrame<Tick>> tickDataMap = dfList.stream().collect(Collectors.toMap(DataFrame::getTimestamp, df -> df));
		barFrame.items().forEach(bar -> 
			tickGenAlgo.generateFrom(bar).forEach(tickEntry -> {
				LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(tickEntry.timestamp()), ZoneId.systemDefault());
				Tick tick = Tick.builder()
						.preClosePrice(bar.preClosePrice())		// 采用分钟K线的模糊值
						.preOpenInterest(bar.preOpenInterest())	// 采用分钟K线的模糊值
						.preSettlePrice(bar.preSettlePrice())	// 采用分钟K线的模糊值
						.highPrice(bar.highPrice())				// 采用分钟K线的模糊值
						.lowPrice(bar.lowPrice())				// 采用分钟K线的模糊值
						.contract(bar.contract())
						.gatewayId(gatewayId)
						.tradingDay(bar.tradingDay())
						.actionDay(bar.actionDay())
						.actionTime(ldt.toLocalTime())
						.actionTimestamp(tickEntry.timestamp())
						.type(TickType.PLAYBACK_TICK)
						.lastPrice(tickEntry.price())
						.askPrice(List.of(tickEntry.askPrice0())) // 仅模拟卖一价
						.bidPrice(List.of(tickEntry.bidPrice0())) // 仅模拟买一价
						.askVolume(List.of(ThreadLocalRandom.current().nextInt(10,500))) // 随机模拟卖一量
						.bidVolume(List.of(ThreadLocalRandom.current().nextInt(10,500))) // 随机模拟买一量
						.channelType(ChannelType.PLAYBACK)
						.volume(bar.volume())								// 采用分钟K线的模糊值
						.volumeDelta(Math.max(1, tickEntry.volume()))		// 采用模拟随机值
						.openInterest(bar.openInterest())					// 采用分钟K线的模糊值
						.openInterestDelta(tickEntry.openInterestDelta())	// 采用模拟随机值
						.turnoverDelta(bar.turnoverDelta())					// 采用分钟K线的模糊值
						.turnover(bar.turnover())							// 采用分钟K线的模糊值
						.build();
				if(!tickDataMap.containsKey(tick.actionTimestamp())) {
					log.warn("Bar: {}", bar);
					log.warn("SampleBar: {}", sample);
					log.warn("ticks: {}", tickEntry);
				}
				tickDataMap.get(tick.actionTimestamp()).add(tick);
			})
		);
		
		return dfList;
	}
	
}
