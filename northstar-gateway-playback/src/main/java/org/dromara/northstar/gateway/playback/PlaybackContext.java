package org.dromara.northstar.gateway.playback;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.ConnectionState;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Notice;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.playback.PlaybackStatus.State;
import org.dromara.northstar.gateway.playback.model.DataFrame;
import org.dromara.northstar.gateway.playback.model.DataFrame.TimeEntry;
import org.dromara.northstar.gateway.playback.model.PlaybackPrecision;
import org.dromara.northstar.kline.BarMerger;
import org.dromara.northstar.kline.KlineGenerator;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;

@Slf4j
public class PlaybackContext {
	
	private FastEventEngine feEngine;
	private final PlaybackGatewayAdapter gateway;
	
	@Getter
	private PlaybackStatus playbackStatus;
	
	private Consumer<PlaybackStatus> onStatusChangeCallback;

	private MarketCenter mktCenter;
	
	private PlaybackDataLoader dataLoader;
	
	private final AtomicBoolean isRunning = new AtomicBoolean();
	
	private final AtomicBoolean isSuspended = new AtomicBoolean();
	
	private ExecutorService exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	
	private AtomicInteger numOfMinutesToPlay;
	private int roundingInt;
	
	private Map<Contract, KlineGenerator> klineGeneratorMap;
	
	private PlaybackPrecision precision;
	
	private ArrayBlockingQueue<DataFrame<Bar>> dataBuffer;
	
	private volatile LocalDateTime dataLoadCursor;
	
	private final AtomicBoolean isLoadThreadActive = new AtomicBoolean();
	
	public PlaybackContext(PlaybackGatewayAdapter gateway, FastEventEngine feEngine, MarketCenter mktCenter) {
		this.feEngine = feEngine;
		this.gateway = gateway;
		this.mktCenter = mktCenter;
		this.init();
	}
	
	public void init() {
		playbackStatus = null;
		Set<IContract> contracts = gateway.getDescription().getSubscribedContracts()
				.stream()
				.map(csi -> mktCenter.getContract(Identifier.of(csi.getId())))
				.collect(Collectors.toSet());
		Assert.isTrue(!startDate().isBefore(preloadDate()), "预热开始日不能迟于回放开始日");
		this.dataLoader = new PlaybackDataLoader(contracts);
		PlaybackGatewaySettings settings = (PlaybackGatewaySettings) gateway.getDescription().getSettings();
		precision = settings.getPrecision();
		this.dataBuffer = new ArrayBlockingQueue<>(1024*4);
		
		// 初始化回放状态
		reset();
		startDataLoadThreadIfNeeded();
	}
	
	private void startDataLoadThreadIfNeeded() {
		if (isLoadThreadActive.compareAndSet(false, true)) {
			exec.execute(() -> {
				try {
					while (true) {
						if (!isRunning.get()) {
							Thread.sleep(100);
							continue;
						}
						LocalDateTime cursor = dataLoadCursor;
						if (cursor == null) {
							cursor = playbackStatus.getRunningDateTime();
						}
						if (cursor.toLocalDate().isAfter(endDate())) {
							Thread.sleep(100);
							continue;
						}
						long startTime = CommonUtils.localDateTimeToMills(cursor);
						long endTime = CommonUtils.localDateTimeToMills(cursor.plusDays(1));
						List<DataFrame<Bar>> data = dataLoader.loadData(startTime, endTime);
						for (DataFrame<Bar> df : data) {
							while (isRunning.get() && !dataBuffer.offer(df, 100, TimeUnit.MILLISECONDS)) {
								// 等待放入队列
							}
							if (!isRunning.get()) {
								break;
							}
						}
						if (isRunning.get()) {
							dataLoadCursor = cursor.plusDays(1);
						}
					}
				} catch (InterruptedException e) {
					log.warn("数据加载线程被中断", e);
				}
			});
		}
	}
	
	private PlaybackGatewaySettings settings() {
		return (PlaybackGatewaySettings) gateway.getDescription().getSettings();
	}
	
	public LocalDate preloadDate() {
		return LocalDate.parse(
				Optional.ofNullable(settings().getPreStartDate()).orElse(settings().getStartDate()),
				DateTimeConstant.D_FORMAT_INT_FORMATTER);
	}
	
	private LocalDate startDate() {
		return LocalDate.parse(settings().getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
	}
	
	private LocalDate endDate() {
		return LocalDate.parse(settings().getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
	}
	
	// 如何处理TICK数据帧
	private Consumer<DataFrame<Tick>> onTickDataCallback = dft -> {
		TimeEntry timeEntry = dft.getTime();
		while(isSuspended.get()) {
			try {
				log.debug("回放进度被挂起等待");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error("", e);
			}
		}
		if(numOfMinutesToPlay != null) {
			return; // 逐K回放模式下，忽略TICK回放
		}
		if(timeEntry.getStart().isBefore(playbackStatus.getRunningDateTime())) {
			return;
		}
		dft.items().forEach(tick -> {
			mktCenter.onTick(tick);
			klineGeneratorMap.get(tick.contract()).updateTick(tick);
			feEngine.emitEvent(NorthstarEventType.TICK, tick);
		});
		try {
			Thread.sleep(playbackStatus.getPlayInterval());
		} catch (InterruptedException e) {
			log.error("等待中断", e);
		}
	};
	
	// 如何处理BAR数据帧
	private Consumer<DataFrame<Bar>> onBarDataCallback = df -> {
		TimeEntry timeEntry = df.getTime();
		if(timeEntry.getStart().isBefore(playbackStatus.getRunningDateTime())) {
			return;
		}
		df.items().forEach(bar -> klineGeneratorMap.get(bar.contract()).updateBar(bar));
		
		playbackStatus.setRunningDateTime(timeEntry.getEnd());
		log.debug("当前回放状态：{}", playbackStatus);
		if(numOfMinutesToPlay != null && numOfMinutesToPlay.decrementAndGet() <= 0 && isRoundingMatch()) {
			pause();
		}
	};
	
	private boolean isRoundingMatch() {
		return playbackStatus.getRunningDateTime().getMinute() % roundingInt == 0;
	}
	
	// 如何处理预加载的BAR数据帧
	private Consumer<DataFrame<Bar>> onPreLoadBarDataCallback = df -> {
		TimeEntry timeEntry = df.getTime();
		if(timeEntry.getStart().isBefore(playbackStatus.getRunningDateTime())) {
			return;
		}
		df.items().forEach(bar -> {
			this.dummyTickOfBar(bar).forEach(tick -> {
				mktCenter.onTick(tick);
				feEngine.emitEvent(NorthstarEventType.TICK, tick);
			});
			
			feEngine.emitEvent(NorthstarEventType.BAR, bar);
			klineGeneratorMap.get(bar.contract()).updateBar(bar);
		});
		playbackStatus.setRunningDateTime(timeEntry.getEnd());
	};
	
	private List<Tick> dummyTickOfBar(Bar bar){
		Tick.TickBuilder builder = Tick.builder()
				.gatewayId(bar.gatewayId())
				.contract(bar.contract())
				.actionDay(bar.actionDay())
				.actionTime(bar.actionTime())
				.actionTimestamp(bar.actionTimestamp())
				.tradingDay(bar.tradingDay())
				.channelType(bar.channelType())
				.type(TickType.PLAYBACK_DUMMY_TICK)
				.bidPrice(Collections.emptyList())
				.bidVolume(Collections.emptyList())
				.askPrice(Collections.emptyList())
				.askVolume(Collections.emptyList())
				.volume(bar.volume())
				.volumeDelta(bar.volumeDelta())
				.openInterest(bar.openInterest())
				.openInterestDelta(bar.openInterestDelta())
				.turnover(bar.turnover())
				.turnoverDelta(bar.turnoverDelta());
	
		return List.of(
					builder.openPrice(bar.openPrice()).highPrice(bar.openPrice()).lowPrice(bar.openPrice()).lastPrice(bar.openPrice()).bidPrice(List.of(bar.openPrice())).askPrice(List.of(bar.openPrice())).build(),
					builder.openPrice(bar.highPrice()).highPrice(bar.highPrice()).lowPrice(bar.highPrice()).lastPrice(bar.highPrice()).bidPrice(List.of(bar.highPrice())).askPrice(List.of(bar.highPrice())).build(),
					builder.openPrice(bar.lowPrice()).highPrice(bar.lowPrice()).lowPrice(bar.lowPrice()).lastPrice(bar.lowPrice()).bidPrice(List.of(bar.lowPrice())).askPrice(List.of(bar.lowPrice())).build(),
					builder.openPrice(bar.closePrice()).highPrice(bar.closePrice()).lowPrice(bar.closePrice()).lastPrice(bar.closePrice()).bidPrice(List.of(bar.closePrice())).askPrice(List.of(bar.closePrice())).build()
				);
	}
	
	public synchronized void start() {
		numOfMinutesToPlay = null;
		doStart();
	}
	
	private void doStart() {
		if(playbackStatus.getState() == State.PLAYING) {
			log.warn("已经处于回放中，不要重复启动");
			return;
		}
		if(playbackStatus.getState() == State.INITIAL) {
			throw new IllegalStateException("需先进行数据预加载");
		}
		
		// 合约过期警告
		PlaybackGatewaySettings settings = (PlaybackGatewaySettings) gateway.getDescription().getSettings();
		LocalDate startDate = LocalDate.parse(settings.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		for(ContractSimpleInfo csi : gateway.getDescription().getSubscribedContracts()) {
			Contract c = mktCenter.getContract(Identifier.of(csi.getId())).contract();
			if(c.lastTradeDate() != null && c.lastTradeDate().isBefore(startDate)) {
				String msg = String.format("合约【%s】在回放时间段内已经过期，将没有数据可以回放，请调整合约或回放时间段", c.name());
				feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
						.status(CommonStatusEnum.COMS_WARN)
						.content(msg)
						.build());
				log.warn("{}", msg);
			}
		}
		
		gateway.setConnection(ConnectionState.CONNECTED);
		playbackStatus.setState(State.PLAYING);
		isRunning.set(true);
		log.info("回放网关 [{}] 连线。当前回放时间状态：{}", gateway.getDescription().getGatewayId(), playbackStatus.getRunningDateTime());
		
		log.info("数据回放阶段：{} -> {}", playbackStatus.getRunningDateTime(), endDate());

		// 回放
		exec.execute(() -> {
			try {
				while(isRunning.get()) {
					DataFrame<Bar> df = dataBuffer.take();
					generateTickFrames(df).forEach(onTickDataCallback::accept);
					onBarDataCallback.accept(df);
				}
			} catch (InterruptedException e) {
				log.warn("回放等待被中断", e);
			}
			if(isRunning.get()) {	
				synchronized (PlaybackContext.this) {
					playbackStatus.setState(State.FINISHED);
					gateway.setConnection(ConnectionState.DISCONNECTED);
					isRunning.set(false);
					log.info("[{}]-历史行情回放已经结束：{}，可通过【复位】重置", gateway.getDescription().getGatewayId(), playbackStatus);
					feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
							.status(CommonStatusEnum.COMS_WARN)
							.content("历史行情回放已经结束，可通过【复位】重置")
							.build());
				}
			}
		});
	}
	
	private List<DataFrame<Tick>> generateTickFrames(DataFrame<Bar> barFrame){
		long nanoPerStep = TimeUnit.MINUTES.toNanos(1) / precision.getNumberOfTickPerBar();
		Map<Long, DataFrame<Tick>> tickDataMap = new HashMap<>();
		barFrame.items().forEach(bar -> 
			precision.getAlgorithm().generateFrom(bar).forEach(tickEntry -> {
				LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(tickEntry.timestamp()), ZoneId.systemDefault());
				tickDataMap.putIfAbsent(tickEntry.timestamp(), new DataFrame<>(ldt, ldt.plusNanos(nanoPerStep)));
				
				Tick tick = Tick.builder()
						.preClosePrice(bar.preClosePrice())		// 采用分钟K线的模糊值
						.preOpenInterest(bar.preOpenInterest())	// 采用分钟K线的模糊值
						.preSettlePrice(bar.preSettlePrice())	// 采用分钟K线的模糊值
						.highPrice(bar.highPrice())				// 采用分钟K线的模糊值
						.lowPrice(bar.lowPrice())				// 采用分钟K线的模糊值
						.contract(bar.contract())
						.gatewayId(ChannelType.PLAYBACK.toString())
						.tradingDay(bar.tradingDay())
						.actionDay(ldt.toLocalDate())
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
				tickDataMap.get(tick.actionTimestamp()).add(tick);
			})
		);
		
		return tickDataMap.entrySet().stream()
				.sorted((a, b) -> a.getKey().compareTo(b.getKey()))
				.map(Entry::getValue)
				.toList();
	}
	
	public synchronized void playNext(Interval interval) {
		LocalDateTime runningDateTime = playbackStatus.getRunningDateTime();
		switch (interval) {
			case M1 -> {
				roundingInt = 1;
				numOfMinutesToPlay = new AtomicInteger(1);
			}
			case M3, M5, M10, M15, M30, H1 -> {
				roundingInt = interval.getMinutes();
				int carry = runningDateTime.getMinute() % roundingInt;
				numOfMinutesToPlay = new AtomicInteger(roundingInt - carry);
			}
			case H2, H4 -> {
				roundingInt = 60;
				int carry = runningDateTime.getMinute() % roundingInt;
				numOfMinutesToPlay = new AtomicInteger(interval.getMinutes() - carry);
			}
			default -> {
				log.warn("无法处理周期{}的逐K回放", interval);
				throw new IllegalArgumentException();
			}
		}
		doStart();
	}
	
	public synchronized void preload() {
		if(playbackStatus.getState() != State.INITIAL) {
			throw new OperationException("error.illegalStateForPreload");
		}
		playbackStatus.setState(State.PRELOADING);
		gateway.setConnection(ConnectionState.CONNECTED);
		log.info("回放网关 [{}] 正在加载预热数据，预热时间段：{} -> {}", gateway.getDescription().getGatewayId(), preloadDate(), startDate());

		// 预热处理
		exec.execute(() -> {
			try {
				LocalDate date = preloadDate();
				while(date.isBefore(startDate())) {
					long startTime = CommonUtils.localDateTimeToMills(date.atStartOfDay());
					long endTime = CommonUtils.localDateTimeToMills(date.plusDays(3).atStartOfDay());
					List<DataFrame<Bar>> data = dataLoader.loadData(startTime, endTime);
					for(DataFrame<Bar> dataFrame : data) {
						onPreLoadBarDataCallback.accept(dataFrame);
					}
					date = date.plusDays(3);
				}
			} catch (InterruptedException e) {
				log.warn("预热加载等待被中断", e);
			}
			synchronized (PlaybackContext.this) {
				playbackStatus.setState(State.PRELOADED);
				gateway.setConnection(ConnectionState.DISCONNECTED);
				isRunning.set(false);
				dataLoadCursor = playbackStatus.getRunningDateTime();
				log.info("回放网关 [{}] 数据预热完毕：{}", gateway.getDescription().getGatewayId(), playbackStatus);
			}
		});
	}
	
	public synchronized void reset() {
		numOfMinutesToPlay = null;
		Set<IContract> contracts = this.gateway.getDescription().getSubscribedContracts()
				.stream()
				.map(csi -> mktCenter.getContract(Identifier.of(csi.getId())))
				.collect(Collectors.toSet());
		klineGeneratorMap = contracts.stream()
				.map(IContract::contract)
				.collect(Collectors.toMap(c -> c, c -> new KlineGenerator(c, bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar))));
		
		dataBuffer.clear();
		LocalDateTime initialDateTime = LocalDateTime.of(preloadDate(), LocalTime.of(0, 0));
		dataLoadCursor = initialDateTime;
		State state =  preloadDate() == startDate() ? State.PRELOADED : State.INITIAL;
		int interval = switch (settings().getSpeed()) {
			case NORMAL -> 500;
			case SPRINT -> 50;
			case RUSH -> 0;
			default -> throw new IllegalArgumentException("Unexpected value: " + settings().getSpeed());
		};
		playbackStatus = PlaybackStatus.builder()
				.runningDateTime(initialDateTime)
				.state(state)
				.playInterval(playbackStatus == null ? interval : playbackStatus.getPlayInterval())
				.onStatusChangeCallback(onStatusChangeCallback)
				.build();
		log.info("回放状态重置：{}", playbackStatus);
	}
	
	public synchronized void setSpeed(int interval) {
		playbackStatus.setPlayInterval(interval);
	}
	
	public synchronized void suspend() {
		isSuspended.set(true);
	}
	
	public synchronized void resume() {
		isSuspended.set(false);
	}
	
	public synchronized void pause() {
		if(!isRunning.get()) {
			return;
		}
		isRunning.set(false);
		gateway.setConnection(ConnectionState.DISCONNECTED);
		playbackStatus.setState(State.PRELOADED);
		log.info("回放网关 [{}] 断开。当前回放时间状态：{}", gateway.getDescription().getGatewayId(), playbackStatus);
	}
	
	public BarMerger getBarMerger(Contract contract, Interval interval) {
		if(!klineGeneratorMap.containsKey(contract)) {
			throw new OperationException("error.loose_contract_subsription", contract.name());
		}
		return klineGeneratorMap.get(contract).getBarMerger(interval);
	}
	
	public void resetBarMerger(Contract contract, Interval interval) {
		if(!klineGeneratorMap.containsKey(contract)) {
			throw new OperationException("error.loose_contract_subsription", contract.name());
		}
		klineGeneratorMap.get(contract).reset(interval);
	}

	public void setOnStatusChangeCallback(Consumer<PlaybackStatus> onStatusChangeCallback) {
		this.onStatusChangeCallback = onStatusChangeCallback;
		playbackStatus.setOnStatusChangeCallback(onStatusChangeCallback);
	}
	
	public void setRunningTime(long timestamp) {
		dataBuffer.clear();
		playbackStatus.setRunningDateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
		dataLoadCursor = playbackStatus.getRunningDateTime();
	}
}
