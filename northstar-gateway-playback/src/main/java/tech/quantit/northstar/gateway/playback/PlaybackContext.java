package tech.quantit.northstar.gateway.playback;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.IContractManager;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.TickType;
import tech.quantit.northstar.common.event.FastEventEngine;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.model.PlaybackRuntimeDescription;
import tech.quantit.northstar.common.utils.MarketDataLoadingUtils;
import tech.quantit.northstar.data.IPlaybackRuntimeRepository;
import tech.quantit.northstar.gateway.playback.ticker.RandomWalkTickSimulation;
import tech.quantit.northstar.gateway.playback.ticker.SimpleCloseSimulation;
import tech.quantit.northstar.gateway.playback.ticker.SimplePriceSimulation;
import tech.quantit.northstar.gateway.playback.ticker.TickEntry;
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
	
	private MarketDataLoadingUtils utils = new MarketDataLoadingUtils();
	
	private PlaybackClock clock;
	
	private PlaybackGatewaySettings settings;
	
	private GatewaySettingField gatewaySettings;
	
	private final LocalDate endDate;
	
	private final IContractManager contractMgr;
	
	private boolean hasPreLoaded;	// 预加载是否被执行过
	
	private Table<ContractField, LocalDate, BarField> tradeDayBarMap = HashBasedTable.create();
	
	private Map<ContractField, TickSimulationAlgorithm> algoMap = new HashMap<>();
	
	// 回放时间戳状态
	private LocalDateTime playbackTimeState;
	
	private boolean isRunning;
	private boolean isLoading;
	private Timer timer;
	
	public PlaybackContext(PlaybackGatewaySettings settings, LocalDateTime currentTimeState, PlaybackClock clock, 
			PlaybackDataLoader loader, FastEventEngine feEngine, IPlaybackRuntimeRepository rtRepo, IContractManager contractMgr) {
		this.settings = settings;
		this.playbackTimeState = currentTimeState;
		this.clock = clock;
		this.loader = loader;
		this.feEngine = feEngine;
		this.rtRepo = rtRepo;
		this.contractMgr = contractMgr;
		this.endDate = LocalDate.parse(settings.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
		
		settings.getUnifiedSymbols()
			.stream()
			.map(contractMgr::getContract)
			.forEach(contract -> 
				algoMap.put(contract, switch(settings.getPrecision()) {
					case EXTREME -> new SimpleCloseSimulation();
					case LOW -> new SimplePriceSimulation();
					case MEDIUM -> new RandomWalkTickSimulation(30, contract.getPriceTick());
					case HIGH -> new RandomWalkTickSimulation(120, contract.getPriceTick());
					default -> throw new IllegalArgumentException("Unexpected value: " + settings.getPrecision());
				})
			);
	}
	
	Map<ContractField, Queue<BarField>> contractBarMap = new HashMap<>();
	Map<ContractField, Queue<TickField>> contractTickMap = new HashMap<>();
	Map<ContractField, BarField> cacheBarMap = new HashMap<>();
	
	/**
	 * 开始回放
	 * @throws InterruptedException 
	 */
	public synchronized void start() {
		feEngine.emitEvent(NorthstarEventType.CONNECTED, gatewaySettings.getGatewayId());
		isRunning = true;
		if(isLoading) {
			feEngine.emitEvent(NorthstarEventType.NOTICE, NoticeField.newBuilder()
					.setContent(String.format("[%s]-当前处于预热阶段，请稍等……", gatewaySettings.getGatewayId()))
					.setStatus(CommonStatusEnum.COMS_WARN)
					.setTimestamp(System.currentTimeMillis())
					.build());
			return;
		}
		long rate = switch (settings.getSpeed()) {
		case NORMAL -> 500;
		case SPRINT -> 10;
		case RUSH -> 1;
		default -> throw new IllegalArgumentException("Unexpected value: " + settings.getSpeed());
		};
		
		log.info("回放网关 [{}] 连线。当前回放时间状态：{}", gatewaySettings.getGatewayId(), playbackTimeState);
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
			private static final int WEEK_DELTA = 20; // 预热时每次加载20周数据
			
			@Override
			public void run() {
				// 预加载数据
				if(!hasPreLoaded) {	
					isLoading = true;
					feEngine.emitEvent(NorthstarEventType.NOTICE, NoticeField.newBuilder()
							.setContent(String.format("[%s]-当前处于预热阶段，请稍等……", gatewaySettings.getGatewayId()))
							.setStatus(CommonStatusEnum.COMS_WARN)
							.setTimestamp(System.currentTimeMillis())
							.build());
					LocalDate preloadStartDate = utils.getFridayOfThisWeek(LocalDate.parse(settings.getPreStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER).minusWeeks(1));
					LocalDate preloadEndDate = playbackTimeState.toLocalDate();
					log.debug("回放网关 [{}] 正在加载预热数据，预热时间段：{} -> {}", gatewaySettings.getGatewayId(), preloadStartDate, preloadEndDate);
					
					playbackTimeState = LocalDateTime.of(preloadEndDate, LocalTime.of(21, 0));
					
					CountDownLatch cdl = new CountDownLatch(settings.getUnifiedSymbols().size());
					settings.getUnifiedSymbols()
						.stream()
						.map(contractMgr::getContract)
						.forEach(contract -> {
							loader.loadTradeDayDataRaw(
									LocalDate.parse(settings.getPreStartDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER),
									LocalDate.parse(settings.getEndDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER),
									contract)
								.forEach(bar -> {
									tradeDayBarMap.put(contract, LocalDate.parse(bar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER), bar);
								});
						
							new Thread(() -> {
								log.info("正在加载 [{}] 合约数据", contract.getUnifiedSymbol());
								LocalDate queryStart = preloadStartDate;
								LocalDate queryEnd = queryStart.plusWeeks(WEEK_DELTA); 
								while(queryStart.isBefore(preloadEndDate)) {
									if(queryEnd.isAfter(preloadEndDate)) {
										queryEnd = preloadEndDate;
									}
									
									loader.loadMinuteDataRaw(queryStart, queryEnd, contract)
										.stream()
										.forEachOrdered(bar -> {
											log.trace("Bar信息： {} {}， {} 价格：{}", bar.getActionDay(), bar.getActionTime(), bar.getUnifiedSymbol(), bar.getClosePrice());
											feEngine.emitEvent(NorthstarEventType.BAR, bar);
										});
									
									queryStart = queryEnd;
									queryEnd = queryEnd.plusWeeks(WEEK_DELTA);
								}
								
								log.debug("回放网关 [{}] 合约 {} 数据预热完毕", gatewaySettings.getGatewayId(), contract.getUnifiedSymbol());
								cdl.countDown();
								isLoading = false;
							}).start();
						});
					
					try {
						cdl.await();
						hasPreLoaded = true;
					} catch (InterruptedException e) {
						log.warn("预热加载等待被中断", e);
					} finally {
						feEngine.emitEvent(NorthstarEventType.NOTICE, NoticeField.newBuilder()
								.setContent(String.format("[%s]-预热阶段结束，请重新连线，正式开始回放。", gatewaySettings.getGatewayId()))
								.setStatus(CommonStatusEnum.COMS_WARN)
								.setTimestamp(System.currentTimeMillis())
								.build());
						stop();
					}
					return;
				}
				
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
				contractTickMap.values().stream()
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
						BarField bar = BarField.newBuilder(itCacheBars.next().getValue()).setGatewayId(gatewaySettings.getGatewayId()).build();
						log.trace("Bar信息： {} {}， {} 价格：{}", bar.getActionDay(), bar.getActionTime(), bar.getUnifiedSymbol(), bar.getClosePrice());
						feEngine.emitEvent(NorthstarEventType.BAR, bar);
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
		
	}
	
	// 按天加载BAR数据
	private void loadBars() {
		log.info("回放网关 [{}] 运行至 {}", gatewaySettings.getGatewayId(), playbackTimeState);
		contractBarMap = settings.getUnifiedSymbols()
			.stream()
			.map(contractMgr::getContract)
			.collect(Collectors.toMap(
					contract -> contract, 
					contract -> new LinkedList<>(loader.loadMinuteData(playbackTimeState, contract))));
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
				TickSimulationAlgorithm algo = algoMap.get(contractMgr.getContract(bar.getUnifiedSymbol()));
				List<TickEntry> ticksOfBar = algo.generateFrom(bar);
				cacheBarMap.put(entry.getKey(), bar);
				contractTickMap.put(entry.getKey(), new LinkedList<>(convertTicks(ticksOfBar, bar)));
			});
	}
	
	private List<TickField> convertTicks(List<TickEntry> ticks, BarField srcBar) {
		ContractField contract = contractMgr.getContract(srcBar.getUnifiedSymbol());
		BarField tradeDayBar = tradeDayBarMap.get(contract, LocalDate.parse(srcBar.getTradingDay(), DateTimeConstant.D_FORMAT_INT_FORMATTER));
		double priceTick = contract.getPriceTick();
		return ticks.stream()
				.map(e -> TickField.newBuilder()
						.setPreClosePrice(tradeDayBar.getPreClosePrice())
						.setPreOpenInterest(tradeDayBar.getPreOpenInterest())
						.setPreSettlePrice(tradeDayBar.getPreSettlePrice())
						.setLowerLimit(tradeDayBar.getPreSettlePrice() * (1 - 0.07))	// 采用7%跌停幅度
						.setUpperLimit(tradeDayBar.getPreSettlePrice() * (1 + 0.07))	// 采用7%涨停幅度
						.setHighPrice(tradeDayBar.getHighPrice())			// 采用K线未来值
						.setLowPrice(tradeDayBar.getLowPrice())				// 采用K线未来值
						.setUnifiedSymbol(srcBar.getUnifiedSymbol())
						.setTradingDay(srcBar.getTradingDay())
						.setStatus(TickType.NORMAL_TICK.getCode())
						.setActionDay(srcBar.getActionDay())
						.setActionTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(e.timestamp()), ZoneId.systemDefault()).format(DateTimeConstant.T_FORMAT_FORMATTER))
						.setActionTimestamp(e.timestamp())
						.setLastPrice(e.price())
						.addAllAskPrice(List.of(e.price() + priceTick, 0D, 0D, 0D, 0D)) // 仅模拟卖一价
						.addAllBidPrice(List.of(e.price() - priceTick, 0D, 0D, 0D, 0D)) // 仅模拟买一价
						.setGatewayId(gatewaySettings.getGatewayId())
						.setVolume(e.volume())								// 采用模拟随机值
						.setOpenInterest(srcBar.getOpenInterest())			// 采用分钟K线的模糊值
						.setOpenInterestDelta(e.openInterestDelta())		// 采用模拟随机值
						.setTurnoverDelta(srcBar.getTurnoverDelta())		// 采用分钟K线的模糊值
						.setTurnover(srcBar.getTurnover())					// 采用分钟K线的模糊值
						.build())
				.toList();
	}
	
	/**
	 * 暂停回放
	 */
	public synchronized void stop() {
		isRunning = false;
		timer.cancel();
		feEngine.emitEvent(NorthstarEventType.DISCONNECTED, gatewaySettings.getGatewayId());
		log.info("回放网关 [{}] 断开。当前回放时间状态：{}", gatewaySettings.getGatewayId(), playbackTimeState);
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
