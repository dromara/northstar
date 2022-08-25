package tech.quantit.northstar.gateway.playback;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
			
			private boolean isBarDataEmpty() {
				int dataQty = contractBarMap.values()
						.stream()
						.mapToInt(Queue::size)
						.reduce(0, (a,b) -> a + b);
				return contractBarMap.isEmpty() || dataQty == 0;
			}
			
			private boolean isTickDataEmpty() {
				int dataQty = contractTickMap.values()
						.stream()
						.mapToInt(Queue::size)
						.reduce(0, (a,b) -> a + b);
				return contractTickMap.isEmpty() || dataQty == 0;
			}
			
			private boolean checkDone() {
				if(playbackTimeState.toLocalDate().isAfter(endDate)) {
					String infoMsg = String.format("[%s]-历史行情回放已经结束，可通过【复位】重置", gatewaySettings.getGatewayId());
					log.info(infoMsg);
					feEngine.emitEvent(NorthstarEventType.NOTICE, NoticeField.newBuilder()
							.setContent(infoMsg)
							.setStatus(CommonStatusEnum.COMS_WARN)
							.setTimestamp(System.currentTimeMillis())
							.build());
					stop();
					return true;
				}
				return false;
			}
			
			private LocalDate lastLoadDate;
			
			@Override
			public void run() {
				while(isTickDataEmpty()) {	
					LocalDate date = playbackTimeState.toLocalDate();
					if(isBarDataEmpty() && !date.equals(lastLoadDate)) {		// 每周加载一次
						loadBars();
						lastLoadDate = date;
					}
					
					if(isTickDataEmpty()) {		// 当Tick数据为空时
						loadTicks();			// 加载Tick数据
					}
					
					if(isTickDataEmpty()) {		// 当加载完仍为空，证明这分钟没有数据 
						playbackTimeState = clock.nextMarketMinute();	// 跳到下一分钟
					}
					
					if(checkDone()) {
						return;
					}
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
				if(isTickDataEmpty()) {
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
					checkDone();
				}
			}
			
		}, 0, rate);
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewaySettings.getGatewayId());
	}
	
	// 按天加载BAR数据
	private void loadBars() {
		contractBarMap = settings.getUnifiedSymbols()
			.stream()
			.map(contractMgr::getContract)
			.collect(Collectors.toMap(
					contract -> contract, 
					contract -> new LinkedList<>(loader.loadData(playbackTimeState, contract)
									.stream()
									.map(bar -> bar.toBuilder().setGatewayId(gatewaySettings.getGatewayId()).build())
									.toList()
							)));
	}
	
	// 按分钟加载TICK数据 
	private void loadTicks() {
		long currentTime = playbackTimeState.toInstant(ZoneOffset.ofHours(8)).toEpochMilli(); 
		contractBarMap.entrySet()
			.stream()
			.filter(entry -> !entry.getValue().isEmpty())
			.filter(entry -> entry.getValue().peek().getActionTimestamp() <= currentTime)
			.forEach(entry -> {
				BarField bar = entry.getValue().poll();
				List<TickField> ticksOfBar = tickerAlgo.generateFrom(bar);
				cacheBarMap.put(entry.getKey(), bar);
				contractTickMap.put(entry.getKey(), new LinkedList<>(ticksOfBar));
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
