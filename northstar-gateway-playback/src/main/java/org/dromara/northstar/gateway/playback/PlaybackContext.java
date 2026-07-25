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
import java.util.concurrent.Executors;
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
	
	private MarketCenter mktCenter;
	
	private final AtomicBoolean isRunning = new AtomicBoolean();
	
	// 用于保证同一时刻只有一个回放任务（连续回放或逐K回放）在消费缓冲区
	private final AtomicBoolean playTaskActive = new AtomicBoolean();
	
	// 回放代际标识，每次启动/暂停/重置/调整时间后递增，用于让阻塞等待数据的任务及时退出，避免死锁
	private final AtomicInteger playbackEpoch = new AtomicInteger();
	
	// 使用平台线程池（守护线程），避免虚拟线程无法被 jstack 排查，以及非守护线程阻塞 JVM 退出
	private ExecutorService exec = Executors.newCachedThreadPool(CommonUtils.platformThreadFactory(getClass()));
	
	private Map<Contract, KlineGenerator> klineGeneratorMap;
	
	private PlaybackPrecision precision;
	
	private PlaybackDataProvider dataProvider;
	
	private volatile LocalDate cachedEndDate;
	
	public PlaybackContext(PlaybackGatewayAdapter gateway, FastEventEngine feEngine, MarketCenter mktCenter) {
		this.feEngine = feEngine;
		this.gateway = gateway;
		this.mktCenter = mktCenter;
		this.init();
	}
	
	public void init() {
		LocalDateTime initialDateTime = LocalDateTime.of(preloadDate(), LocalTime.of(0, 0));
		playbackStatus = PlaybackStatus.builder()
				.runningDateTime(initialDateTime)
				.state(State.INITIAL)
				.playInterval(settings().getSpeed().getPauseInterval())
				.build();
		Assert.isTrue(!startDate().isBefore(preloadDate()), "预热开始日不能迟于回放开始日");
		this.dataProvider = new PlaybackDataProvider(this);
		PlaybackGatewaySettings settings = (PlaybackGatewaySettings) gateway.getDescription().getSettings();
		precision = settings.getPrecision();
		
		// 初始化回放状态
		reset();
	}
	
	Set<IContract> playbackContracts(){
		return gateway.getDescription().getSubscribedContracts()
				.stream()
				.map(csi -> mktCenter.getContract(Identifier.of(csi.getId())))
				.collect(Collectors.toSet());
	}
	
	private PlaybackGatewaySettings settings() {
		return (PlaybackGatewaySettings) gateway.getDescription().getSettings();
	}
	
	public LocalDate preloadDate() {
		return LocalDate.parse(
				Optional.ofNullable(settings().getPreStartDate()).orElse(settings().getStartDate()),
				DateTimeConstant.D_FORMAT_INT_FORMATTER);
	}
	
	LocalDate startDate() {
		return LocalDate.parse(settings().getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
	}
	
	LocalDate endDate() {
		if(cachedEndDate == null) {
			LocalDate endDate = LocalDate.parse(settings().getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
			// 回放结束日不能大于等于当前日期，避免回放到尚未收盘的当日数据
			LocalDate maxEndDate = LocalDate.now().minusDays(1);
			if(endDate.isAfter(maxEndDate)) {
				log.warn("回放结束日 [{}] 不能大于等于当前日期，自动调整为 [{}]", endDate, maxEndDate);
				endDate = maxEndDate;
			}
			// 缓存计算结果，避免加载线程轮询调用时重复打印警告日志
			cachedEndDate = endDate;
		}
		return cachedEndDate;
	}
	
	// 如何处理TICK数据帧
	private void onTickDataCallback(DataFrame<Tick> dft, boolean hasInterval){
		TimeEntry timeEntry = dft.getTime();
		if(timeEntry.getStart().isBefore(playbackStatus.getRunningDateTime())) {
			return;
		}
		dft.items().forEach(tick -> {
			mktCenter.onTick(tick);
			klineGeneratorMap.get(tick.contract()).updateTick(tick);
			feEngine.emitEvent(NorthstarEventType.TICK, tick);
		});
		
		if(!hasInterval) {
			// 逐K回放时不需要停顿
			return;
		}
		try {
			Thread.sleep(playbackStatus.getPlayInterval());
		} catch (InterruptedException e) {
			log.error("等待中断", e);
		}
	};
	
	// 如何处理BAR数据帧
	private void onBarDataCallback(DataFrame<Bar> df) {
		TimeEntry timeEntry = df.getTime();
		if(timeEntry.getStart().isBefore(playbackStatus.getRunningDateTime())) {
			return;
		}
		df.items().forEach(bar -> klineGeneratorMap.get(bar.contract()).updateBar(bar));
	};
	
	// 如何处理预加载的BAR数据帧
	private void onPreLoadBarDataCallback(DataFrame<Bar> df) {
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
	
	public synchronized void start() {
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
		
		if(!playTaskActive.compareAndSet(false, true)) {
			log.warn("已有回放任务在进行中，忽略本次启动请求");
			return;
		}
		playbackEpoch.incrementAndGet();
		gateway.setConnection(ConnectionState.CONNECTED);
		playbackStatus.setState(State.PLAYING);
		isRunning.set(true);
		log.info("回放网关 [{}] 连线。当前回放时间状态：{}", gateway.getDescription().getGatewayId(), playbackStatus.getRunningDateTime());
		
		LocalDateTime endDateTime = endDate().plusDays(1).atStartOfDay();
		log.info("数据回放阶段：{} -> {}", playbackStatus.getRunningDateTime(), endDateTime);
		
		dataProvider.startSupply();

		// 回放
		exec.execute(() -> {
			try {
				while(isRunning.get() && playbackStatus.getRunningDateTime().isBefore(endDateTime)) {
					// 使用带超时的 poll 代替 take，确保暂停/重置后循环能及时退出，避免任务泄漏与重复消费
					DataFrame<Bar> df = dataProvider.getBuffer().poll(100, TimeUnit.MILLISECONDS);
					if(df == null) {
						if(dataProvider.isExhausted()) {
							if(dataProvider.hasMoreData()) {
								// 加载线程异常退出但仍有数据未加载，重启数据供应，避免回放永久卡住
								dataProvider.startSupply();
							} else {
								// 数据已全部加载并消费完毕（如结束日当天无数据），提前正常结束，避免永久空转
								break;
							}
						}
						continue;
					}
					generateTickFrames(df).forEach(dft -> onTickDataCallback(dft, true));
					onBarDataCallback(df);
					playbackStatus.setRunningDateTime(df.getTime().getEnd());
					log.debug("当前回放状态：{}", playbackStatus);
				}
			} catch (InterruptedException e) {
				log.warn("回放等待被中断", e);
				Thread.currentThread().interrupt();
			} finally {
				playTaskActive.set(false);
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
	
	/**
	 * 逐K回放。
	 * 注意：本方法不能声明为 synchronized，阻塞等待数据时绝不能持有实例锁，
	 * 否则缓冲区耗尽后会与 start/pause/reset 等 synchronized 方法形成死锁，导致整个回放卡死。
	 * @param interval
	 */
	public void playNext(Interval interval) {
		if(!playTaskActive.compareAndSet(false, true)) {
			log.warn("已有回放任务在进行中，忽略本次逐K回放请求");
			return;
		}
		final int epoch = playbackEpoch.get();
		try {
			dataProvider.startSupply();
			for(int i=0; i < interval.getMinutes(); i++) {
				if(playbackEpoch.get() != epoch) {
					// 期间发生了启动/暂停/重置/调时，立即退出，避免与新的回放任务并发消费
					log.info("回放状态已变更，逐K回放提前结束");
					return;
				}
				// 使用带超时的 poll 代替 take，避免无限期阻塞
				DataFrame<Bar> df = dataProvider.getBuffer().poll(100, TimeUnit.MILLISECONDS);
				if(df == null) {
					if(dataProvider.isExhausted()) {
						log.info("回放数据已耗尽，逐K回放结束");
						return;
					}
					i--;	// 未取到数据不算消耗一根K线
					continue;
				}
				generateTickFrames(df).forEach(dft -> onTickDataCallback(dft, false));
				onBarDataCallback(df);
				playbackStatus.setRunningDateTime(df.getTime().getEnd());
				log.debug("当前回放状态：{}", playbackStatus);
			}
		} catch(InterruptedException e) {
			log.warn("等待中断");
			Thread.currentThread().interrupt();
		} finally {
			playTaskActive.set(false);
		}
	}
	
	public synchronized void preload() {
		if(playbackStatus.getState() != State.INITIAL) {
			throw new OperationException("error.illegalStateForPreload");
		}
		playbackStatus.setState(State.PRELOADING);
		gateway.setConnection(ConnectionState.CONNECTED);
		LocalDateTime preloadStart = preloadDate().atStartOfDay();
		LocalDateTime preloadEnd = startDate().atStartOfDay();
		log.info("回放网关 [{}] 正在加载预热数据，预热时间段：{} -> {}", gateway.getDescription().getGatewayId(), preloadStart, preloadEnd);

		final ArrayBlockingQueue<DataFrame<Bar>> preloadBuffer = dataProvider.loadPreloadData();
		// 预热处理
		exec.execute(() -> {
			try {
				while(playbackStatus.getRunningDateTime().isBefore(preloadEnd)) {
					if(playbackStatus.getState() != State.PRELOADING) {
						// 预热期间状态被外部变更（如重置），放弃预热
						log.warn("预热期间回放状态被变更，预热中止");
						return;
					}
					// 使用带超时的 poll 代替 take，避免预热期数据缺失（如节假日）时永久阻塞
					DataFrame<Bar> df = preloadBuffer.poll(100, TimeUnit.MILLISECONDS);
					if(df == null) {
						if(dataProvider.isPreloadSupplyDone() && preloadBuffer.isEmpty()) {
							// 预热数据已全部供应完毕，但运行时间未到达回放开始日（预热期内无更多数据），直接跳到回放开始时间
							playbackStatus.setRunningDateTime(preloadEnd);
							break;
						}
						continue;
					}
					onPreLoadBarDataCallback(df);
					playbackStatus.setRunningDateTime(df.getTime().getEnd());
				}
			} catch (InterruptedException e) {
				log.warn("预热加载等待被中断", e);
				Thread.currentThread().interrupt();
			}
			synchronized (PlaybackContext.this) {
				playbackStatus.setState(State.PRELOADED);
				gateway.setConnection(ConnectionState.DISCONNECTED);
				isRunning.set(false);
				dataProvider.setRunningTime(playbackStatus.getRunningDateTime());
				log.info("回放网关 [{}] 数据预热完毕：{}", gateway.getDescription().getGatewayId(), playbackStatus);
			}
		});
	}
	
	public synchronized void reset() {
		playbackEpoch.incrementAndGet();
		klineGeneratorMap = playbackContracts().stream().map(IContract::contract)
				.collect(Collectors.toMap(c -> c, c -> new KlineGenerator(c, bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar))));
		
		LocalDateTime initialDateTime = LocalDateTime.of(preloadDate(), LocalTime.of(0, 0));
		State state =  preloadDate().equals(startDate()) ? State.PRELOADED : State.INITIAL;
		playbackStatus.setRunningDateTime(initialDateTime);
		playbackStatus.setState(state);
		log.info("回放状态重置：{}", playbackStatus);
	}
	
	public synchronized void setSpeed(int interval) {
		playbackStatus.setPlayInterval(interval);
	}
	
	public synchronized void pause() {
		playbackEpoch.incrementAndGet();
		if(!isRunning.get()) {
			return;
		}
		isRunning.set(false);
		dataProvider.pauseSupply();
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
		playbackStatus.setOnStatusChangeCallback(onStatusChangeCallback);
	}
	
	public void setRunningTime(long timestamp) {
		playbackEpoch.incrementAndGet();
		LocalDateTime runningDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
		playbackStatus.setRunningDateTime(runningDateTime);
		dataProvider.setRunningTime(runningDateTime);
	}
}
