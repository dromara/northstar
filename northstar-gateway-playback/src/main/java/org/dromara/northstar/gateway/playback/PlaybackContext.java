package org.dromara.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.PlaybackRuntimeDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Notice;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.playback.model.DataFrame;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;

@Slf4j
public class PlaybackContext implements IPlaybackContext{
	
	private IPlaybackRuntimeRepository rtRepo;
	
	private FastEventEngine feEngine;
	
	private String gatewayId;
	
	private AtomicBoolean hasPreLoaded = new AtomicBoolean();	// 预加载是否被执行过
	private AtomicBoolean isRunning = new AtomicBoolean();
	
	// 回放时间戳状态
	private LocalDateTime playbackState;
	
	private Set<IContract> contracts;
	
	private IMarketCenter mktCenter;
	
	private Runnable stopCallback;
	
	private PlaybackDataLoader dataLoader;
	
	private final AtomicLong pauseInterval = new AtomicLong();
	
	private final PlaybackGatewaySettings settings;
	
	private ExecutorService exec = CommonUtils.newThreadPerTaskExecutor(getClass());
	
	public PlaybackContext(GatewayDescription gd, LocalDateTime currentTimeState, 
			FastEventEngine feEngine, IPlaybackRuntimeRepository rtRepo, IContractManager contractMgr) {
		this.rtRepo = rtRepo;
		this.feEngine = feEngine;
		this.mktCenter = (IMarketCenter) contractMgr;
		this.playbackState = currentTimeState;
		this.gatewayId = gd.getGatewayId();
		this.settings = (PlaybackGatewaySettings) gd.getSettings();
		this.contracts = settings.getPlayContracts().stream()
				.map(csi -> contractMgr.getContract(ChannelType.PLAYBACK, csi.getUnifiedSymbol()))
				.collect(Collectors.toSet());
		this.dataLoader = new PlaybackDataLoader(gatewayId, contracts, settings.getPrecision());
		int interval = switch (settings.getSpeed()) {
			case NORMAL -> 500;
			case SPRINT -> 5;
			case RUSH -> 0;
			default -> throw new IllegalArgumentException("Unexpected value: " + settings.getSpeed());
		};
		this.pauseInterval.set(interval);
	}
	
	// 如何处理TICK数据帧
	private Consumer<DataFrame<Tick>> onTickDataCallback = dft -> {
		long timestamp = dft.getTimestamp();
		LocalDateTime curState = CommonUtils.millsToLocalDateTime(timestamp);
		if(curState.isBefore(playbackState) || !isRunning()) {
			return;
		}
		dft.items().forEach(tick -> {
			mktCenter.onTick(tick);
			feEngine.emitEvent(NorthstarEventType.TICK, tick);
		});
		try {
			Thread.sleep(pauseInterval.get());
		} catch (InterruptedException e) {
			log.error("等待中断", e);
		}
	};
	
	// 如何处理BAR数据帧
	private BiConsumer<DataFrame<Bar>, Boolean> onBarDataCallback = (df, flag) -> {
		long timestamp = df.getTimestamp();
		LocalDateTime curState = CommonUtils.millsToLocalDateTime(timestamp);
		if(curState.isBefore(playbackState) || !isRunning()) {
			return;
		}
		df.items().forEach(bar -> feEngine.emitEvent(NorthstarEventType.BAR, bar));
		
		playbackState = curState;
		// 收到检查点标志位时，保存回放状态
		if(Boolean.TRUE.equals(flag)) {
			log.info("当前回放状态：{}", playbackState);
			rtRepo.save(PlaybackRuntimeDescription.builder()
					.gatewayId(gatewayId)
					.playbackTimeState(playbackState)
					.build());
		}
	};
	
	// 如何处理预加载的BAR数据帧
	private Consumer<DataFrame<Bar>> onPreLoadBarDataCallback = df -> df.items().forEach(bar -> {
		this.dummyTickOfBar(bar).forEach(tick -> feEngine.emitEvent(NorthstarEventType.TICK, tick));
		feEngine.emitEvent(NorthstarEventType.BAR, bar);
	});
	
	private List<Tick> dummyTickOfBar(Bar bar){
		Tick.TickBuilder builder = Tick.builder()
				.gatewayId(bar.gatewayId())
				.contract(bar.contract())
				.actionDay(bar.actionDay())
				.actionTime(bar.actionTime())
				.actionTimestamp(bar.actionTimestamp())
				.tradingDay(bar.tradingDay())
				.channelType(bar.channelType())
				.type(TickType.PLAYBACK_TICK)
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
					builder.lastPrice(bar.openPrice()).build(),
					builder.lastPrice(bar.highPrice()).build(),
					builder.lastPrice(bar.lowPrice()).build(),
					builder.lastPrice(bar.closePrice()).build()
				);
	}
	
	@Override
	public void start() {
		CompletableFuture<Void> runningJob;
		isRunning.set(true);
		log.info("回放网关 [{}] 连线。当前回放时间状态：{}", gatewayId, playbackState);
		
		LocalDate preStart = LocalDate.parse(settings.getPreStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		LocalDate preEnd = playbackState.toLocalDate().minusDays(1);
		
		if(!hasPreLoaded.get() && preStart.isBefore(preEnd)) {
			log.info("回放网关 [{}] 正在加载预热数据，预热时间段：{} -> {}", gatewayId, preStart, preEnd);
			feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
				.content(String.format("[%s]-当前处于预热阶段，请稍等……", gatewayId))
				.status(CommonStatusEnum.COMS_WARN)
				.build());
			runningJob = dataLoader.preload(preStart, preEnd, onPreLoadBarDataCallback);
			hasPreLoaded.set(true);
			
			// 预热完毕的处理
			exec.execute(() -> {
				try {
					runningJob.get();
				} catch (InterruptedException | ExecutionException e) {
					log.warn("预热加载等待被中断", e);
				}
				log.debug("回放网关 [{}] 数据预热完毕", gatewayId);
				feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
						.content(String.format("[%s]-预热阶段结束，请重新连线，正式开始回放。", gatewayId))
						.status(CommonStatusEnum.COMS_WARN)
						.build());
				stop();
			});
			
			return;		//若进入预加载，则需要二次连线才能进入回放
		}
		
		LocalDate start = playbackState.toLocalDate();
		LocalDate end = LocalDate.parse(settings.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		log.info("数据回放阶段：{} -> {}", start, end);
		runningJob = dataLoader.load(start, end, () -> !isRunning.get(), onTickDataCallback, onBarDataCallback);
		
		// 回放结束的处理
		exec.execute(() -> {
			try {
				runningJob.get();
			} catch (InterruptedException | ExecutionException e) {
				log.warn("回放等待被中断", e);
			}
			if(isRunning.get()) {				
				String infoMsg = String.format("[%s]-历史行情回放已经结束，可通过【复位】重置", gatewayId);
				log.info(infoMsg);
				feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
						.content(infoMsg)
						.status(CommonStatusEnum.COMS_WARN)
						.build());
			}
			stop();
		});
	}
	
	@Override
	public void stop() {
		isRunning.set(false);
		log.info("回放网关 [{}] 断开。当前回放时间状态：{}", gatewayId, playbackState);
		if(Objects.nonNull(stopCallback)) {
			stopCallback.run();
		}
	}
	
	@Override
	public boolean isRunning() {
		return isRunning.get();
	}

	@Override
	public void onStopCallback(Runnable callback) {
		stopCallback = callback;
	}

}
