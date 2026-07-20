package org.dromara.northstar.module;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.BacktestModuleTradeDetailsVO;
import org.dromara.northstar.common.model.BacktestModuleVO;
import org.dromara.northstar.common.model.ContractChartDescription;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.ExplorerTaskDescription;
import org.dromara.northstar.common.model.ExplorerTaskVO;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDealRecordVO;
import org.dromara.northstar.common.model.TimeValues;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.gateway.playback.ticker.SimplePriceSimulation;
import org.dromara.northstar.gateway.playback.ticker.TickEntry;
import org.dromara.northstar.kline.BarMerger;
import org.dromara.northstar.kline.BarMergerFactory;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleContext;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 一个runner代表了一个策略探索任务
 * 它包含了多个探索模组实例
 */
@Slf4j
@Builder
public class BacktestRunner {
	
	private ExplorerTaskDescription taskDescription;
	
	private LocalDate fromDate;
	
	private LocalDate toDate;
	
	private LocalDate preloadDate;
	
	private NorthstarDataSource dataSource;
	
	private Map<Contract, List<IModule>> contractModulesMap;
	
	private List<IModule> moduleList;
	
	private IAccountRepository accountRepo;
	
	private LocalDate cursorDate;
	
	private final AtomicBoolean isRunning = new AtomicBoolean();
	
	/* 回测图表分段缓存。由于模组内部RingArray只保留最近500根Bar，需要定期保存快照，查询时再合并 */
	private final Map<String, List<ModuleChartDescription>> moduleChartCache = new ConcurrentHashMap<>();

	private final Map<String, Integer> moduleBarCounterMap = new ConcurrentHashMap<>();

	private final Map<Contract, EnumMap<Interval, BarMerger>> contractBarMergerMap = new HashMap<>();

	public void execute() {
		if(isRunning.get()) {
			return;
		}
		isRunning.set(true);
		
		resetChartCache();
		initBarMergers();

		for(IModule m : moduleList) {
			m.setEnabled(false);
			accountRepo.removeDealRecords(m.getModuleContext().getModuleDescription().getName());
		}
		
		CompletableFuture.runAsync(() -> {
			try {
		        doRun(preloadDate, fromDate);
		        log.info("预热结束");
		        // 图表只保留实测阶段的数据，预热数据仅用于指标与策略状态准备
		        resetChartCache();

		        for(IModule m : moduleList) {
		            m.setEnabled(true);
		        }

		        doRun(fromDate, toDate);
		    } catch (Throwable e) {
		        log.error("策略探索任务执行失败", e);
		        isRunning.set(false);
		    }
		});
	}
	
	private void resetChartCache() {
		moduleChartCache.clear();
		moduleBarCounterMap.clear();
		for(IModule module : moduleList) {
			String moduleName = module.getModuleContext().getModuleDescription().getName();
			moduleChartCache.put(moduleName, new CopyOnWriteArrayList<>());
			moduleBarCounterMap.put(moduleName, 0);
		}
	}

	private void initBarMergers() {
		contractBarMergerMap.clear();
		contractModulesMap.forEach((contract, modules) -> {
			EnumMap<Interval, BarMerger> mergerMap = new EnumMap<>(Interval.class);
			Arrays.stream(Interval.values())
					.filter(interval -> interval != Interval.M1)
					.forEach(interval -> {
						BarMerger merger = BarMergerFactory.newInstance(interval, contract);
						merger.addListener(mergedBar -> modules.forEach(module -> module.getModuleContext().onBar(mergedBar)));
						mergerMap.put(interval, merger);
					});
			contractBarMergerMap.put(contract, mergerMap);
		});
	}

	private int progress() {
		double progressRatio = 0;
		if(cursorDate != null) {			
			long totalDays = ChronoUnit.DAYS.between(preloadDate, toDate);
			long passedDays = Math.clamp(ChronoUnit.DAYS.between(preloadDate, cursorDate), 0, totalDays);
			progressRatio = 1.0 * passedDays / totalDays;
		}
		
		if(isRunning.get()) {
			progressRatio = Math.max(0.01, progressRatio); 
		}
		return (int) (progressRatio * 100);
	}
	
	private void doRun(LocalDate from, LocalDate to) {
		SimplePriceSimulation tickSimulator = new SimplePriceSimulation();
		cursorDate = from;
		while(cursorDate.isBefore(to.plusDays(1))) {
			LocalDate loadStart = cursorDate;
			LocalDate loadEnd = cursorDate.plusDays(6);
			for(Contract contract : contractModulesMap.keySet()) {
				List<Bar> bars = dataSource.getData(contract, Interval.M1, loadStart, loadEnd);
				List<IModule> modules = contractModulesMap.get(contract);
				for(Bar bar : bars.reversed()) {
					if(bar.actionDay().isAfter(to)) {
						break;
					}
					List<TickEntry> tickEntryList = tickSimulator.generateFrom(bar);
					for(IModule module : modules) {
						tickEntryList.forEach(entry -> module.getModuleContext().onTick(makeTick(entry, bar)));
						module.getModuleContext().onBar(bar);
					}
					mergeBars(contract, bar);
					modules.forEach(this::cacheChartIfNecessary);
				}
			}
			cursorDate = loadStart.plusWeeks(1);
		}
		
		isRunning.set(false);
		cursorDate = to.plusDays(1);
	}
	
	private void mergeBars(Contract contract, Bar bar) {
		contractBarMergerMap.getOrDefault(contract, new EnumMap<>(Interval.class))
				.values()
				.forEach(merger -> merger.onBar(bar));
	}

	private void cacheChartIfNecessary(IModule module) {
		IModuleContext ctx = module.getModuleContext();
		String moduleName = ctx.getModuleDescription().getName();
		int barCount = moduleBarCounterMap.merge(moduleName, 1, Integer::sum);
		if(barCount % BacktestModuleFactory.BUF_SIZE == 0) {
			moduleChartCache.get(moduleName).add(ctx.getChartDescription());
		}
	}

	private Tick makeTick(TickEntry tickEntry, Bar bar) {
		LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(tickEntry.timestamp()), ZoneId.systemDefault());
		
		return Tick.builder()
				.preClosePrice(bar.preClosePrice())		// 采用分钟K线的模糊值
				.preOpenInterest(bar.preOpenInterest())	// 采用分钟K线的模糊值
				.preSettlePrice(bar.preSettlePrice())	// 采用分钟K线的模糊值
				.highPrice(bar.highPrice())				// 采用分钟K线的模糊值
				.lowPrice(bar.lowPrice())				// 采用分钟K线的模糊值
				.contract(bar.contract())
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
	}
	
	public List<IModule> getModules(){
		return moduleList;
	}
	
	public ExplorerTaskVO getExplorerTask() {
		return ExplorerTaskVO.builder()
				.description(taskDescription)
				.progress(progress())
				.build();
	}
	
	public List<BacktestModuleVO> getBacktestModule() {
		return moduleList.stream()
				.map(m -> {
					IModuleContext ctx = m.getModuleContext();
					String moduleName = ctx.getModuleDescription().getName();
					List<DealRecord> dealRecords = accountRepo.findDealRecords(moduleName);
					double profitAvg = dealRecords.stream().mapToDouble(DealRecord::getDealProfit).filter(x -> x > 0).average().orElse(0);
					double lossAvg = dealRecords.stream().mapToDouble(DealRecord::getDealProfit).filter(x -> x <= 0).average().orElse(0);
					double winning = dealRecords.stream().mapToDouble(DealRecord::getDealProfit).filter(x -> x > 0).count();
					double totalProfit = dealRecords.stream().mapToDouble(DealRecord::getDealProfit).sum();
					double totalFees = dealRecords.stream().mapToDouble(DealRecord::getTransactionFee).sum();
					double initBalance = ctx.getModuleDescription().getInitBalance();
					double unrealizedProfit = ((ModuleAccount) ctx.getModuleAccount()).getPositions().stream()
							.mapToDouble(Position::positionProfit).sum();
					double balance = initBalance + totalProfit + unrealizedProfit - totalFees;
					
					double maxDrawdown = 0;
					double peak = initBalance;
					if(!dealRecords.isEmpty()) {
						double cumulative = 0;
						for(DealRecord rec : dealRecords) {
							cumulative += rec.getDealProfit() - rec.getTransactionFee();
							double equity = initBalance + cumulative;
							if(equity > peak) {
								peak = equity;
							}
							double dd = peak - equity;
							if(dd > maxDrawdown) {
								maxDrawdown = dd;
							}
						}
					}
					double currentEquity = initBalance + totalProfit + unrealizedProfit - totalFees;
					if(currentEquity > peak) {
						peak = currentEquity;
					}
					double currentDD = peak - currentEquity;
					if(currentDD > maxDrawdown) {
						maxDrawdown = currentDD;
					}
					double maxDrawdownPercentage = peak > 0 ? (maxDrawdown / peak) * 100 : 0;
					
					double earningRate = (balance / initBalance) - 1;
					double annualizedReturn = earningRate / ChronoUnit.DAYS.between(fromDate, toDate) * 365;
					return BacktestModuleVO.builder()
							.moduleName(moduleName)
							.contract(ContractSimpleInfo.from(m.getModuleContext().bindedContracts().getFirst()))
							.tradeSize(ctx.getDefaultVolume())
							.balance(balance)
							.transactionFees((int)totalFees)
							.realizedProfit(totalProfit)
							.unrealizedProfit(unrealizedProfit)
							.maxDrawdown(maxDrawdown)
							.maxDrawdownPercentage(maxDrawdownPercentage)
							.annualizedReturn(annualizedReturn)
							.winRate(dealRecords.isEmpty() ? 0 : winning / dealRecords.size())
							.profitLossRatio(lossAvg == 0 ? 0 : profitAvg / Math.abs(lossAvg))
							.totalTrades(dealRecords.size())
							.build();
				})
				.toList();
	}
	
	public BacktestModuleTradeDetailsVO getBacktestModuleTradeDetails(String moduleName) {
		List<ModuleDealRecordVO> dealRecords = accountRepo.findDealRecords(moduleName)
				.stream()
				.map(rec -> {
					TradeField openTrade = rec.convertOpenTrade();
					TradeField closeTrade = rec.convertCloseTrade();
					PositionDirectionEnum dir = switch(openTrade.getDirection()) {
						case D_Buy -> PositionDirectionEnum.PD_Long;
						case D_Sell -> PositionDirectionEnum.PD_Short;
						default -> throw new IllegalArgumentException();
					};
					double profit = switch(openTrade.getDirection()) {
						case D_Buy -> (closeTrade.getPrice() - openTrade.getPrice()) * closeTrade.getVolume() * closeTrade.getContract().getMultiplier();
						case D_Sell -> (openTrade.getPrice() - closeTrade.getPrice()) * closeTrade.getVolume() * closeTrade.getContract().getMultiplier();
						default -> throw new IllegalArgumentException();
					};
					return ModuleDealRecordVO.builder()
							.contractId(openTrade.getContract().getContractId())
							.name(openTrade.getContract().getName())
							.symbol(openTrade.getContract().getSymbol())
							.dir(dir)
							.transactionFee((int)rec.getTransactionFee())
							.vol(closeTrade.getVolume())
							.profit((int)profit)
							.openPrice(String.format("%.2f", openTrade.getPrice()))
							.openTimestamp(openTrade.getTradeTimestamp())
							.closePrice(String.format("%.2f", closeTrade.getPrice()))
							.closeTimestamp(closeTrade.getTradeTimestamp())
							.build();
				})
				.toList();
		ModuleChartDescription currentChart = moduleList.stream()
				.filter(m -> m.getModuleContext().getModuleDescription().getName().equals(moduleName))
				.findAny()
				.map(m -> m.getModuleContext().getChartDescription())
				.orElse(null);
		List<ModuleChartDescription> chartSections = new ArrayList<>(moduleChartCache.getOrDefault(moduleName, List.of()));
		if(currentChart != null) {
			chartSections.add(currentChart);
		}
		return BacktestModuleTradeDetailsVO.builder()
				.dealRecords(dealRecords)
				.chartData(mergeChartDescriptions(chartSections))
				.build();
	}

	static ModuleChartDescription mergeChartDescriptions(List<ModuleChartDescription> chartSections) {
		if(chartSections.isEmpty()) {
			return null;
		}
		Map<Interval, List<ContractChartDescription>> dataMap = new EnumMap<>(Interval.class);
		for(Interval interval : Interval.values()) {
			Map<String, List<ContractChartDescription>> chartsByContract = new LinkedHashMap<>();
			for(ModuleChartDescription section : chartSections) {
				if(section.getData() == null) {
					continue;
				}
				section.getData().getOrDefault(interval, List.of()).forEach(chart ->
						chartsByContract.computeIfAbsent(chart.getContract().getId(), _ -> new ArrayList<>()).add(chart));
			}
			dataMap.put(interval, chartsByContract.values().stream()
					.map(BacktestRunner::mergeContractCharts)
					.toList());
		}
		return ModuleChartDescription.builder()
				.data(dataMap)
				.strategyInfos(chartSections.getLast().getStrategyInfos())
				.build();
	}

	private static ContractChartDescription mergeContractCharts(List<ContractChartDescription> charts) {
		ContractChartDescription latest = charts.getLast();
		Map<Long, TimeValues> valuesByTimestamp = new LinkedHashMap<>();
		charts.stream()
				.flatMap(chart -> chart.getTimedValues().stream())
				.forEach(tv -> valuesByTimestamp.put(tv.getTimestamp(), tv));
		return ContractChartDescription.builder()
				.contract(latest.getContract())
				.fields(latest.getFields())
				.indicators(latest.getIndicators())
				.timedValues(new ArrayList<>(valuesByTimestamp.values()))
				.build();
	}
	
}
