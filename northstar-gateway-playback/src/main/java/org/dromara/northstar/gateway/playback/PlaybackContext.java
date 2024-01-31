package org.dromara.northstar.gateway.playback;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.event.FastEventEngine;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.common.model.GatewayDescription;
import org.dromara.northstar.common.model.PlaybackRuntimeDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Notice;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.utils.DateTimeUtils;
import org.dromara.northstar.data.IPlaybackRuntimeRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.gateway.IMarketCenter;
import org.dromara.northstar.gateway.playback.utils.ContractDataLoader;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;

@Slf4j
public class PlaybackContext implements IPlaybackContext{
	
	private IPlaybackRuntimeRepository rtRepo;
	
	private FastEventEngine feEngine;
	
	private PlaybackGatewaySettings settings;
	
	private String gatewayId;
	
	private boolean hasPreLoaded;	// 预加载是否被执行过
	
	// 回放时间戳状态
	private LocalDate playbackTradeDate;
	private LocalDateTime playbackTimeState;
	
	private final LocalDate playbackEndDate;
	
	private Set<ContractDataLoader> loaders;
	
	private IMarketCenter mktCenter;
	
	private Runnable stopCallback;
	
	private boolean isRunning;
	private Timer timer;
	
	public PlaybackContext(GatewayDescription gd, LocalDateTime currentTimeState, 
			FastEventEngine feEngine, IPlaybackRuntimeRepository rtRepo, IContractManager contractMgr) {
		this.rtRepo = rtRepo;
		this.feEngine = feEngine;
		this.mktCenter = (IMarketCenter) contractMgr;
		this.playbackTimeState = currentTimeState;
		this.gatewayId = gd.getGatewayId();
		this.settings = (PlaybackGatewaySettings) gd.getSettings();
		this.playbackEndDate = LocalDate.parse(settings.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		this.loaders = settings.getPlayContracts().stream()
				.map(csi -> contractMgr.getContract(ChannelType.PLAYBACK, csi.getUnifiedSymbol()))
				.map(c -> new ContractDataLoader(gatewayId, c, settings.getPrecision()))
				.collect(Collectors.toSet());
	}
	
	@Override
	public void start() {
		isRunning = true;
		long rate = switch (settings.getSpeed()) {
		case NORMAL -> 500;
		case SPRINT -> 25;
		case RUSH -> 1;
		default -> throw new IllegalArgumentException("Unexpected value: " + settings.getSpeed());
		};
		
		log.info("回放网关 [{}] 连线。当前回放时间状态：{}", gatewayId, playbackTimeState);
		timer = new Timer("Playback", true);
		timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				if(!isRunning()) {
					return;
				}
				// 预加载数据
				if(!hasPreLoaded) {
					if(StringUtils.equals(settings.getStartDate(), settings.getPreStartDate())) {
						hasPreLoaded = true;
						playbackTradeDate = LocalDate.parse(settings.getStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
						return;
					}
					feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
							.content(String.format("[%s]-当前处于预热阶段，请稍等……", gatewayId))
							.status(CommonStatusEnum.COMS_WARN)
							.build());
					LocalDate preloadStartDate = LocalDate.parse(settings.getPreStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
					LocalDate preloadEndDate = playbackTimeState.toLocalDate();
					playbackTradeDate = preloadEndDate.plusDays(1);
					log.debug("回放网关 [{}] 正在加载预热数据，预热时间段：{} -> {}", gatewayId, preloadStartDate, preloadEndDate);
					
					playbackTimeState = LocalDateTime.of(preloadEndDate, DateTimeUtils.fromCacheTime(21, 0));
					CountDownLatch cdl = new CountDownLatch(loaders.size());
					
					loaders.stream().forEach(loader -> 
						new Thread(() -> {
							LocalDate loadDate = preloadStartDate;
							while(!loadDate.isAfter(preloadEndDate)) {
								if(loadDate.getDayOfWeek() == DayOfWeek.SATURDAY || loadDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
									loadDate = loadDate.plusDays(1);
									continue;
								}
								loader.loadBars(loadDate);
								while(loader.hasMoreBar()) {
									Bar bar = loader.nextBar(true);
									feEngine.emitEvent(NorthstarEventType.BAR, bar);
								}
								loadDate = loadDate.plusDays(1);
							}
							log.debug("回放网关 [{}] 合约 {} 数据预热完毕", gatewayId, loader.getContract().contract().unifiedSymbol());
							cdl.countDown();
						}).start()
					);
					
					try {
						cdl.await();
						hasPreLoaded = true;
					} catch (InterruptedException e) {
						log.warn("预热加载等待被中断", e);
					} finally {
						feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
								.content(String.format("[%s]-预热阶段结束，请重新连线，正式开始回放。", gatewayId))
								.status(CommonStatusEnum.COMS_WARN)
								.build());
						stop();
					}
					return;
				}
				
				// 加载数据
				if(loaders.stream().filter(ContractDataLoader::hasMoreBar).count() == 0) {
					rtRepo.save(PlaybackRuntimeDescription.builder()
							.gatewayId(gatewayId)
							.playbackTimeState(playbackTimeState)
							.build());
					
					if(playbackTradeDate.isAfter(playbackEndDate)) {
						stop();
						String infoMsg = String.format("[%s]-历史行情回放已经结束，可通过【复位】重置", gatewayId);
						log.info(infoMsg);
						feEngine.emitEvent(NorthstarEventType.NOTICE, Notice.builder()
								.content(infoMsg)
								.status(CommonStatusEnum.COMS_WARN)
								.build());
						return;
					}
					
					loaders.forEach(loader -> loader.loadBarsAndTicks(playbackTradeDate));
					playbackTradeDate = playbackTradeDate.plusDays(1);
				}

				// 回放数据
				loaders.stream().filter(ContractDataLoader::hasMoreBar).forEach(loader -> {
					Bar bar = loader.nextBar(false);
					if(loader.hasMoreTick()) {
						Tick tick = loader.nextTick(false);
						if(tick.actionTimestamp() <= bar.actionTimestamp()) {
							Tick t = loader.nextTick(true);
							mktCenter.onTick(t);
							feEngine.emitEvent(NorthstarEventType.TICK, t);
							return;
						}
					}
					feEngine.emitEvent(NorthstarEventType.BAR, loader.nextBar(true));
					LocalDateTime ldt = LocalDateTime.of(bar.actionDay(), bar.actionTime());
					if(playbackTimeState.isBefore(ldt)) {
						playbackTimeState = ldt;
					}
				});
			}
		}, 0, rate);
	}
	
	@Override
	public void stop() {
		isRunning = false;
		timer.cancel();
		log.info("回放网关 [{}] 断开。当前回放时间状态：{}", gatewayId, playbackTimeState);
		if(Objects.nonNull(stopCallback)) {
			stopCallback.run();
		}
	}
	
	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void onStopCallback(Runnable callback) {
		stopCallback = callback;
	}

}
