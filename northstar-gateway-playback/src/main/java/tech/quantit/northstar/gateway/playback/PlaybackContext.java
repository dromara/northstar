package tech.quantit.northstar.gateway.playback;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.PlaybackRuntimeDescription;
import tech.quantit.northstar.common.model.PlaybackSettings;
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
public class PlaybackContext {
	
	private IPlaybackRuntimeRepository rtRepo;
	
	private FastEventEngine feEngine;
	
	private PlaybackDataLoader loader;
	
	private TickSimulationAlgorithm tickerAlgo;
	
	private PlaybackClock clock;
	
	private PlaybackSettings settings;
	
	private GatewaySettingField gatewaySettings;
	
	private final LocalDate endDate;
	
	// 回放时间戳状态
	private long playbackTimeState;
	
	private boolean isRunning;
	private Timer timer;
	
	public PlaybackContext(PlaybackSettings settings, LocalDateTime currentTimeState, PlaybackClock clock, TickSimulationAlgorithm tickerAlgo,
			PlaybackDataLoader loader, FastEventEngine feEngine, IPlaybackRuntimeRepository rtRepo) {
		this.settings = settings;
		this.playbackTimeState = currentTimeState.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
		this.clock = clock;
		this.tickerAlgo = tickerAlgo;
		this.loader = loader;
		this.feEngine = feEngine;
		this.rtRepo = rtRepo;
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
		case SPRINT -> 50;
		default -> throw new IllegalArgumentException("Unexpected value: " + settings.getSpeed());
		};
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				if(contractBarMap.isEmpty()) {
					loadBars();
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
						feEngine.emitEvent(NorthstarEventType.BAR, itCacheBars.next().getValue());
						itCacheBars.remove();
					}
					playbackTimeState = clock.nextMarketMinute();
					LocalDateTime newDT = LocalDateTime.ofInstant(Instant.ofEpochMilli(playbackTimeState), ZoneId.systemDefault());
					rtRepo.save(PlaybackRuntimeDescription.builder()
							.playbackGatewayId(gatewaySettings.getGatewayId())
							.playbackTimeState(newDT)
							.build());
					// 回放结束后，自动停机
					if(newDT.toLocalDate().isAfter(endDate)) {
						feEngine.emitEvent(NorthstarEventType.NOTICE, NoticeField.newBuilder()
								.setContent(String.format("[%s] 回放已经结束", gatewaySettings.getGatewayId()))
								.setStatus(CommonStatusEnum.COMS_INFO)
								.setTimestamp(playbackTimeState)
								.build());
						stop();
					}
				}
			}
			
		}, 0, rate);
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewaySettings.getGatewayId());
	}
	
	private void loadBars() {
		settings.getContractGroups()
			.stream()
			.map(contractGroup -> loader.loadData(playbackTimeState, contractGroup))
			.forEach(sourceMap -> {
				for(Entry<ContractField, List<BarField>> e : sourceMap.entrySet()) {
					contractBarMap.put(e.getKey(), new LinkedList<>(e.getValue()));
				}
			});
	}
	
	private void loadTicks() {
		contractBarMap.entrySet()
			.stream()
			.filter(entry -> !entry.getValue().isEmpty())
			.filter(entry -> entry.getValue().peek().getActionTimestamp() < playbackTimeState)
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
