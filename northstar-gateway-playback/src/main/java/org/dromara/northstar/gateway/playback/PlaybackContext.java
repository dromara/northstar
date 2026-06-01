package org.dromara.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import org.dromara.northstar.kline.KlineGenerator;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;

@Slf4j
public class PlaybackContext {
	
	private FastEventEngine feEngine;
	private final PlaybackGatewayAdapter gateway;
	
	@Getter
	private PlaybackStatus playbackStatus;
	
	@Setter
	private Consumer<PlaybackStatus> onStatusChangeCallback;

	private MarketCenter mktCenter;
	
	private PlaybackDataLoader dataLoader;
	
	private final AtomicBoolean isRunning = new AtomicBoolean();
	
	private final AtomicBoolean isSuspended = new AtomicBoolean();
	
	private ExecutorService exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	
	private AtomicInteger numOfMinutesToPlay;
	private int roundingInt;
	
	private GlobalPlaybackDataDetector dataDetector;	// 用来检测数据回放情况
	
	private Map<Contract, KlineGenerator> klineGeneratorMap;
	
	public PlaybackContext(PlaybackGatewayAdapter gateway, FastEventEngine feEngine, MarketCenter mktCenter, GlobalPlaybackDataDetector dataDetector) {
		this.feEngine = feEngine;
		this.gateway = gateway;
		this.mktCenter = mktCenter;
		this.dataDetector = dataDetector;
		this.init();
	}
	
	public void init() {
		playbackStatus = null;
		Set<IContract> contracts = this.gateway.getDescription().getSubscribedContracts()
				.stream()
				.map(csi -> mktCenter.getContract(Identifier.of(csi.getId())))
				.collect(Collectors.toSet());
		Assert.isTrue(!startDate().isBefore(preloadDate()), "预热开始日不能迟于回放开始日");
		this.dataLoader = new PlaybackDataLoader(gateway.getDescription().getGatewayId(), contracts, settings().getPrecision());
		
		klineGeneratorMap = contracts.stream()
				.map(IContract::contract)
				.collect(Collectors.toMap(c -> c, c -> new KlineGenerator(c, bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar))));
		// 初始化回放状态
		reset();
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
		if(timeEntry.getStart().isBefore(playbackStatus.getRunningDateTime()) || !isRunning.get()) {
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
		if(timeEntry.getStart().isBefore(playbackStatus.getRunningDateTime()) || !isRunning.get()) {
			return;
		}
		df.items().forEach(bar -> {
//			feEngine.emitEvent(NorthstarEventType.BAR, bar);
			klineGeneratorMap.get(bar.contract()).updateBar(bar);
		});
		
		playbackStatus.setRunningDateTime(timeEntry.getEnd());
		if(onStatusChangeCallback != null) {
			onStatusChangeCallback.accept(playbackStatus);
		}
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
		if(onStatusChangeCallback != null) {
			onStatusChangeCallback.accept(playbackStatus);
		}
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
		if(onStatusChangeCallback != null) {
			onStatusChangeCallback.accept(playbackStatus);
		}
		isRunning.set(true);
		log.info("回放网关 [{}] 连线。当前回放时间状态：{}", gateway.getDescription().getGatewayId(), playbackStatus.getRunningDateTime());
		
		LocalDate start = playbackStatus.getRunningDateTime().toLocalDate();
		log.info("数据回放阶段：{} -> {}", playbackStatus.getRunningDateTime(), endDate());
		CompletableFuture<Void> runningJob = dataLoader.load(start.minusDays(1), endDate(), () -> !isRunning.get(), onTickDataCallback, onBarDataCallback);
		
		// 回放结束的处理
		exec.execute(() -> {
			try {
				runningJob.get();
			} catch (InterruptedException | ExecutionException e) {
				log.warn("回放等待被中断", e);
				if(e instanceof ExecutionException) {
					feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
							.status(CommonStatusEnum.COMS_ERROR)
							.content(e.getCause().getLocalizedMessage()).build());
				}
			}
			if(isRunning.get()) {	
				synchronized (PlaybackContext.this) {
					playbackStatus.setState(State.FINISHED);
					if(onStatusChangeCallback != null) {
						onStatusChangeCallback.accept(playbackStatus);
					}
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
		if(onStatusChangeCallback != null) {
			onStatusChangeCallback.accept(playbackStatus);
		}
		gateway.setConnection(ConnectionState.CONNECTED);
		log.info("回放网关 [{}] 正在加载预热数据，预热时间段：{} -> {}", gateway.getDescription().getGatewayId(), preloadDate(), startDate());
		CompletableFuture<Void> runningJob = dataLoader.preload(preloadDate(), startDate(), onPreLoadBarDataCallback);
		
		// 预热完毕的处理
		exec.execute(() -> {
			try {
				runningJob.get();
				while(System.currentTimeMillis() - dataDetector.getLastEventTime() < 500) {
					Thread.sleep(1000);
				}
			} catch (InterruptedException | ExecutionException e) {
				log.warn("预热加载等待被中断", e);
				if(e instanceof ExecutionException) {
					feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
							.status(CommonStatusEnum.COMS_ERROR)
							.content(e.getCause().getLocalizedMessage()).build());
				}
			}
			synchronized (PlaybackContext.this) {
				playbackStatus.setState(State.PRELOADED);
				if(onStatusChangeCallback != null) {
					onStatusChangeCallback.accept(playbackStatus);
				}
				gateway.setConnection(ConnectionState.DISCONNECTED);
				isRunning.set(false);
				log.info("回放网关 [{}] 数据预热完毕：{}", gateway.getDescription().getGatewayId(), playbackStatus);
			}
		});
	}
	
	public synchronized void reset() {
		numOfMinutesToPlay = null;
		LocalDateTime initialDateTime = LocalDateTime.of(preloadDate(), LocalTime.of(0, 0));
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
				.build();
		log.info("回放状态重置：{}", playbackStatus);
		if(onStatusChangeCallback != null) {
			onStatusChangeCallback.accept(playbackStatus);
		}
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
		if(onStatusChangeCallback != null) {
			onStatusChangeCallback.accept(playbackStatus);
		}
		log.info("回放网关 [{}] 断开。当前回放时间状态：{}", gateway.getDescription().getGatewayId(), playbackStatus);
	}
	
}
