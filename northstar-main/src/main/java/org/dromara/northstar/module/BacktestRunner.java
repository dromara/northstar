package org.dromara.northstar.module;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.TickType;
import org.dromara.northstar.common.model.BacktestModuleTradeDetailsVO;
import org.dromara.northstar.common.model.BacktestModuleVO;
import org.dromara.northstar.common.model.ContractChartDescription;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.ExplorerTaskDescription;
import org.dromara.northstar.common.model.ExplorerTaskVO;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDealRecordVO;
import org.dromara.northstar.common.model.TimeValues;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.data.jdbc.ModuleDealRecordRepository;
import org.dromara.northstar.data.jdbc.entity.ModuleDealRecordDO;
import org.dromara.northstar.gateway.mktdata.NorthstarDataSource;
import org.dromara.northstar.gateway.playback.ticker.SimplePriceSimulation;
import org.dromara.northstar.gateway.playback.ticker.TickEntry;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleContext;

import lombok.Builder;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 一个runner代表了一个策略探索任务
 * 它包含了多个探索模组实例
 */
@Builder
public class BacktestRunner {
	
	private ExplorerTaskDescription taskDescription;
	
	private LocalDate fromDate;
	
	private LocalDate toDate;
	
	private LocalDate preloadDate;
	
	private NorthstarDataSource dataSource;
	
	private Map<Contract, List<IModule>> contractModulesMap;
	
	private List<IModule> moduleList;
	
	private ModuleDealRecordRepository dealRecordRepo;
	
	private LocalDate cursorDate;
	
	private final AtomicBoolean isRunning = new AtomicBoolean();
	
	private final Map<IModule, LinkedList<ModuleChartDescription>> moduleChartCache = new HashMap<>();
	
	public void execute() {
		if(isRunning.get()) {
			return;
		}
		isRunning.set(true);
		
		for(IModule m : moduleList) {
			m.setEnabled(false);
			dealRecordRepo.deleteByModuleName(m.getModuleContext().getModuleDescription().getName());
		}
		
		CompletableFuture.runAsync(() -> {
			// 预热阶段
			doRun(preloadDate, fromDate);
			
			// 实测阶段
			for(IModule m : moduleList) {
				m.setEnabled(true);
				moduleChartCache.put(m, new LinkedList<>());
				((BacktestModule)m).addListener(chart -> moduleChartCache.get(m).add(chart));
			}
			doRun(fromDate, toDate);
		});
	}
	
	private int progress() {
		double progressRatio = 0;
		if(cursorDate != null) {			
			long totalDays = ChronoUnit.DAYS.between(preloadDate, toDate);
			long passedDays = Math.min(totalDays, Math.max(0, ChronoUnit.DAYS.between(preloadDate, cursorDate)));
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
				List<Bar> bars = dataSource.getMinutelyData(contract, loadStart, loadEnd);
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
				}
			}
			cursorDate = loadStart.plusWeeks(1);
		}
		
		isRunning.set(false);
		cursorDate = to.plusDays(1);
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
					List<ModuleDealRecord> dealRecords = dealRecordRepo.findByModuleName(moduleName)
							.stream().map(ModuleDealRecordDO::convertTo).toList();
					double profitAvg = dealRecords.stream().mapToDouble(ModuleDealRecord::getDealProfit).filter(x -> x > 0).average().orElse(0);
					double lossAvg = dealRecords.stream().mapToDouble(ModuleDealRecord::getDealProfit).filter(x -> x <= 0).average().orElse(0);
					double winning = dealRecords.stream().mapToDouble(ModuleDealRecord::getDealProfit).filter(x -> x > 0).count();
					double totalProfit = dealRecords.stream().mapToDouble(ModuleDealRecord::getDealProfit).sum();
					double totalFees = dealRecords.stream().mapToDouble(ModuleDealRecord::getTransactionFee).sum();
					ModuleAccountRuntimeDescription mard = ctx.getRuntimeDescription().getModuleAccountRuntime();
					double unrealizedProfit = mard.unrealizedProfit();
					double balance = mard.getInitBalance() + totalProfit + unrealizedProfit - totalFees;
					
					double earningRate = (balance / ctx.getModuleDescription().getInitBalance()) - 1;
					double annualizedReturn = earningRate / ChronoUnit.DAYS.between(fromDate, toDate) * 365;
					return BacktestModuleVO.builder()
							.moduleName(moduleName)
							.contract(ContractSimpleInfo.from(m.getModuleContext().bindedContracts().getFirst()))
							.period(ctx.numOfMinPerMergedBar())
							.tradeSize(ctx.getDefaultVolume())
							.balance(balance)
							.transactionFees((int)totalFees)
							.realizedProfit(totalProfit)
							.unrealizedProfit(mard.unrealizedProfit())
							.maxDrawdown(mard.getMaxDrawdown())
							.maxDrawdownPercentage(mard.getMaxDrawdownPercentage())
							.annualizedReturn(annualizedReturn)
							.winRate(dealRecords.isEmpty() ? 0 : winning / dealRecords.size())
							.profitLossRatio(lossAvg == 0 ? 0 : profitAvg / Math.abs(lossAvg))
							.totalTrades(dealRecords.size())
							.build();
				})
				.toList();
	}
	
	public BacktestModuleTradeDetailsVO getBacktestModuleTradeDetails(String moduleName) {
		List<ModuleDealRecordVO> dealRecords = dealRecordRepo.findByModuleName(moduleName)
				.stream()
				.map(ModuleDealRecordDO::convertTo)
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
		AtomicReference<ContractChartDescription> mcd = new AtomicReference<>();
		moduleChartCache.entrySet().stream()
			.filter(e -> e.getKey().getModuleContext().getModuleDescription().getName().equals(moduleName))
			.findAny()
			.ifPresent(e -> {
				ModuleChartDescription bufChart = e.getKey().getModuleContext().getChartDescription();
				ContractChartDescription contractChart = bufChart.getData().getFirst();
				List<ModuleChartDescription> sections = e.getValue();
				List<ContractChartDescription> charts = sections.stream().map(item -> item.getData().getFirst()).toList();
				List<TimeValues> values = charts.stream().flatMap(ccd -> ccd.getTimedValues().stream()).toList();
				if(values.isEmpty()) {
					return;
				}
				List<TimeValues> bufValues = contractChart.getTimedValues().stream().filter(tv -> tv.getTimestamp() > values.getLast().getTimestamp()).toList();
				List<TimeValues> mergedValues = new ArrayList<>(values.size() + bufValues.size());
				mergedValues.addAll(values);
				mergedValues.addAll(bufValues);
				mcd.set(ContractChartDescription.builder()
						.contract(charts.getFirst().getContract())
						.fields(charts.getFirst().getFields())
						.indicators(charts.getFirst().getIndicators())
						.timedValues(mergedValues)
						.build());
			});
		return BacktestModuleTradeDetailsVO.builder()
				.dealRecords(dealRecords)
				.chartData(mcd.get())
				.build();
	}
	
}
