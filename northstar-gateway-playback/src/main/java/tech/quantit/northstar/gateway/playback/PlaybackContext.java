package tech.quantit.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.PlaybackRuntimeDescription;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.gateway.playback.ticker.TickSimulationAlgorithm;
import tech.quantit.northstar.gateway.playback.utils.PlaybackClock;
import tech.quantit.northstar.gateway.playback.utils.PlaybackDataLoader;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.NoticeField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 回放上下文，执行回放任务
 * @author KevinHuangwl
 *
 */
@Slf4j
public class PlaybackContext {
	
	private IPlaybackRuntimeRepository rtRepo;
	
	private FastEventEngine feEngine;
	
	private PlaybackDataLoader loader;
	
	private TickSimulationAlgorithm tickerAlgo;
	
	private PlaybackClock clock;
	
	private PlaybackGatewaySettings settings;
	
	private GatewaySettingField gatewaySettings;
	
	private final LocalDate endDate;
	
	private final IContractManager contractMgr;
	
	// 回放时间戳状态
	private LocalDateTime playbackTimeState;
	
	private boolean isRunning;
	private Timer timer;
	
	public PlaybackContext(PlaybackGatewaySettings settings, LocalDateTime currentTimeState, PlaybackClock clock, TickSimulationAlgorithm tickerAlgo,
			PlaybackDataLoader loader, FastEventEngine feEngine, IPlaybackRuntimeRepository rtRepo, IContractManager contractMgr) {
		this.settings = settings;
		this.playbackTimeState = currentTimeState;
		this.clock = clock;
		this.tickerAlgo = tickerAlgo;
		this.loader = loader;
		this.feEngine = feEngine;
		this.rtRepo = rtRepo;
		this.contractMgr = contractMgr;
		this.endDate = LocalDate.parse(settings.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		
	}
	
	Map<ContractField, Queue<BarField>> contractBarMap = new HashMap<>();
	Map<ContractField, Queue<TickField>> contractTickMap = new HashMap<>();
	Map<ContractField, BarField> cacheBarMap = new HashMap<>();
	
	/**
	 * 开始回放
	 */
	public void start() {
		isRunning = true;
		long rate = switch (settings.getSpeed()) {
		case NORMAL -> 500;
		case SPRINT -> 10;
		default -> throw new IllegalArgumentException("Unexpected value: " + settings.getSpeed());
		};
		
		log.debug("回放网关 [{}] 开始回放。当前时间：{}", gatewaySettings.getGatewayId(), playbackTimeState);
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			
			private LocalDate lastLoadDate;
			
			private boolean hasLoaded(LocalDate date) {
				return date.equals(lastLoadDate);
			}
			
			@Override
			public void run() {
				LocalDate loadDate = playbackTimeState.toLocalDate();
				if(contractBarMap.isEmpty() && !hasLoaded(loadDate)) {
					loadBars();
					lastLoadDate = loadDate;
				}
				
				if(contractTickMap.isEmpty()) {
					loadTicks();
				}
				
				// 每次分发TICK
				contractTickMap.values().parallelStream()
					.filter(tickQ -> !tickQ.isEmpty())
					.forEach(tickQ -> feEngine.emitEvent(NorthstarEventType.TICK, tickQ.poll()));
				
				Iterator<Entry<ContractField, Queue<BarField>>> itBars = contractBarMap.entrySet().iterator();
				while(itBars.hasNext()) {
					if(itBars.next().getValue().isEmpty()) {
						itBars.remove();
					}
				}
				
				Iterator<Entry<ContractField, Queue<TickField>>> itTicks = contractTickMap.entrySet().iterator();
				while(itTicks.hasNext()) {
					if(itTicks.next().getValue().isEmpty()) {
						itTicks.remove();
					}
				}
				
				// 每分钟分发BAR
				if(contractTickMap.isEmpty()) {
					Iterator<Entry<ContractField, BarField>> itCacheBars = cacheBarMap.entrySet().iterator();
					while(itCacheBars.hasNext()) {
						feEngine.emitEvent(NorthstarEventType.BAR, BarField.newBuilder(itCacheBars.next().getValue()).setGatewayId(gatewaySettings.getGatewayId()).build());
						itCacheBars.remove();
					}
					playbackTimeState = clock.nextMarketMinute();
					rtRepo.save(PlaybackRuntimeDescription.builder()
							.gatewayId(gatewaySettings.getGatewayId())
							.playbackTimeState(playbackTimeState)
							.build());
					// 回放结束后，自动停机
					if(playbackTimeState.toLocalDate().isAfter(endDate)) {
						feEngine.emitEvent(NorthstarEventType.NOTICE, NoticeField.newBuilder()
								.setContent(String.format("[%s] 回放已经结束", gatewaySettings.getGatewayId()))
								.setStatus(CommonStatusEnum.COMS_WARN)
								.setTimestamp(System.currentTimeMillis())
								.build());
						stop();
					}
				}
			}
			
		}, 0, rate);
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewaySettings.getGatewayId());
	}
	
	private void loadBars() {
		contractBarMap = settings.getUnifiedSymbols()
			.stream()
			.map(contractMgr::getContract)
			.collect(Collectors.toMap(
					contract -> contract, 
					contract -> new LinkedList<>(loader.loadData(playbackTimeState, contract))));
	}
	
	private void loadTicks() {
		long currentTime = playbackTimeState.toInstant(ZoneOffset.ofHours(8)).toEpochMilli(); 
		contractBarMap.entrySet()
			.stream()
			.filter(entry -> !entry.getValue().isEmpty())
			.filter(entry -> entry.getValue().peek().getActionTimestamp() < currentTime)
			.forEach(entry -> {
				BarField bar = entry.getValue().poll();
				cacheBarMap.put(entry.getKey(), bar);
				contractTickMap.put(entry.getKey(), new LinkedList<>(tickerAlgo.generateFrom(bar)));
			});
	}
	
	/**
	 * 暂停回放
	 */
	public void stop() {
		isRunning = false;
		timer.cancel();
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, gatewaySettings.getGatewayId());
		log.debug("回放网关 [{}] 结束回放。当前时间：{}", gatewaySettings.getGatewayId(), playbackTimeState);
	}
	
	/**
	 * 是否在运行
	 * @return
	 */
	public boolean isRunning() {
		return isRunning;
	}
	
	public void setGatewaySettings(GatewaySettingField gatewaySettings) {
		this.gatewaySettings = gatewaySettings;
	}
}
