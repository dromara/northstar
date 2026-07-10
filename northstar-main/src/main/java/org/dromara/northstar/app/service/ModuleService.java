package org.dromara.northstar.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.dromara.northstar.account.AccountManager;
import org.dromara.northstar.account.GatewayManager;
import org.dromara.northstar.app.restful.SseStreamController;
import org.dromara.northstar.common.IDataSource;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.ModuleType;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.exception.NoSuchModuleException;
import org.dromara.northstar.common.exception.OperationException;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.DealRecord;
import org.dromara.northstar.common.model.DynamicParams;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountRuntimeVO;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDealRecordVO;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleMockTradeDTO;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.ModuleRuntimeVO;
import org.dromara.northstar.common.model.ModuleVO;
import org.dromara.northstar.common.model.PositionVO;
import org.dromara.northstar.common.model.StrategyMetaInfo;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.CommonUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IAccountRepository;
import org.dromara.northstar.data.IMarketDataRepository;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.TradeGateway;
import org.dromara.northstar.gateway.contract.OptionChainContract;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.gateway.sim.trade.AccountPosition;
import org.dromara.northstar.kline.KlineGenerator;
import org.dromara.northstar.module.ArbitrageModuleContext;
import org.dromara.northstar.module.ModuleAccount;
import org.dromara.northstar.module.ModuleContext;
import org.dromara.northstar.module.ModuleManager;
import org.dromara.northstar.module.PlaybackModuleContext;
import org.dromara.northstar.module.TradeModule;
import org.dromara.northstar.strategy.DynamicParamsAware;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.support.utils.ModuleDataLoader;
import org.dromara.northstar.support.utils.NamingValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.protobuf.InvalidProtocolBufferException;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TradeField;

@Slf4j
@Order(2000)
@Service
public class ModuleService implements CommandLineRunner {
	
	@Autowired
	private ModuleManager moduleMgr;
	
	@Autowired
	private GatewayManager gatewayMgr;
	
	@Autowired
	private AccountManager accountMgr;

	@Autowired
	private IModuleRepository moduleRepo;
	
	@Autowired
	private IMarketDataRepository mdRepo;
	
	@Autowired
	private IAccountRepository accountRepo;
	
	@Autowired
	private MarketCenter mktCenter;
	
	@Autowired
	private SseStreamController sseController;

	@Transactional
	public ModuleVO createModule(ModuleDescription md) throws Exception {
		validateSettings(md);
		Identifier moduleID = Identifier.of(md.getName());
		if(moduleMgr.contains(moduleID)) {
			throw new OperationException("error.duplicated_module", md.getName());
		}
		if(!NamingValidator.isValid(md.getName())) {
			throw new OperationException("error.illegalCharater", md.getName());
		}
		log.info("创建模组 [{}] ", md.getName());
		ModuleRuntimeDescription mrd = ModuleRuntimeDescription.builder()
				.moduleName(md.getName())
				.storeObject(new JSONObject())
				.build();
		
		TradeModule module;
		module = loadModule(md, mrd);
		moduleRepo.saveSettings(md);
		moduleRepo.saveRuntime(mrd);
		moduleMgr.add(moduleID, module);
		
		log.info("模组创建成功");
		sseController.sendModuleStatus();
		return ModuleVO.builder()
				.description(md)
				.enabled(module.getModuleContext().isEnabled())
				.state(module.getModuleContext().getState())
				.build();
	}

	@Transactional
	public ModuleVO updateModule(ModuleDescription md, boolean reset) throws Exception {
		validateSettings(md);
		if(reset) {
			log.info("重置模组 [{}]", md.getName());
			removeModule(md.getName());
			return createModule(md);
		}
		log.info("更新模组 [{}]", md.getName());
		TradeModule module;
		validateChange(md);
		unloadModule(md.getName());
		module = loadModule(md, moduleRepo.findRuntimeByName(md.getName()));
		moduleMgr.add(Identifier.of(md.getName()), module);
		moduleRepo.saveSettings(md);
		
		log.info("模组更新成功");
		sseController.sendModuleStatus();
		return ModuleVO.builder()
				.description(md)
				.enabled(module.getModuleContext().isEnabled())
				.state(module.getModuleContext().getState())
				.build();
	}
	
	private void validateSettings(ModuleDescription md) {
		// 回测模组只能使用绑定了回放网关的账户
		List<ContractSimpleInfo> playbackContracts = md.getBindedContracts().stream().filter(csi -> csi.getChannelType() == ChannelType.PLAYBACK).toList();
		if(!playbackContracts.isEmpty() && md.getUsage() != ModuleUsage.PLAYBACK) {
			throw new OperationException("error.playback_binding_error");
		}
		
		// 实盘模组不能使用SIM网关账户
		List<TradeGateway> simGateways = md.getBindedAccounts().stream()
				.map(account -> accountMgr.get(Identifier.of(account)))
				.map(acc -> acc.getTradeGateway())
				.filter(gw -> gw.getDescription().getChannelType() == ChannelType.SIM)
				.toList();
		if(md.getUsage() == ModuleUsage.PROD && !simGateways.isEmpty()) {
			throw new OperationException("error.prod_module_binding_error");
		}
	}
	
	// 更新合法性校验：持仓状态下，模组不允许修改绑定的账户与合约信息
	private void validateChange(ModuleDescription md) {
		IModule module = moduleMgr.get(Identifier.of(md.getName()));
		ModuleDescription md0 = module.getModuleContext().getModuleDescription();
		if(!module.getModuleContext().getState().isEmpty()) {
			Set<String> accountNames = md.getBindedAccounts().stream().collect(Collectors.toSet());
			Set<String> accountNames0 = md0.getBindedAccounts().stream().collect(Collectors.toSet());
			Set<String> bindedContracts = md.getBindedContracts().stream().map(ContractSimpleInfo::getUnifiedSymbol).collect(Collectors.toSet());
			Set<String> bindedContracts0 = md0.getBindedContracts().stream().map(ContractSimpleInfo::getUnifiedSymbol).collect(Collectors.toSet());
			if(!accountNames.equals(accountNames0) || !bindedContracts.equals(bindedContracts0)) {
				throw new OperationException("error.update_forbidden_when_holding");
			}
		}
	}
	
	private void unloadModule(String moduleName) {
		moduleMgr.remove(Identifier.of(moduleName));
		moduleRepo.deleteSettingsByName(moduleName);
	}
	
	private ModuleAccount makeAccount(ModuleDescription md, ModuleRuntimeDescription mrd) throws InvalidProtocolBufferException {
		Map<Contract, IAccount> contractAccountMap = new HashMap<>();
		
		Table<Contract, PositionDirectionEnum, List<Trade>> positionTradesTbl = accountRepo.findHoldingTrades(md.getName());
		Table<Contract, PositionDirectionEnum, AccountPosition> positionTbl = HashBasedTable.create();
		md.getBindedContracts().forEach(csi -> {
			Contract c = mktCenter.getContract(Identifier.of(csi.getId())).contract();
			
			List<Trade> longTrades = Optional.ofNullable(positionTradesTbl.get(c, PositionDirectionEnum.PD_Long)).orElse(Collections.emptyList());
			List<Trade> shortTrades = Optional.ofNullable(positionTradesTbl.get(c, PositionDirectionEnum.PD_Short)).orElse(Collections.emptyList());
			positionTbl.put(c, PositionDirectionEnum.PD_Long, new AccountPosition(md.getName(), c, PositionDirectionEnum.PD_Long, accountRepo, longTrades));
			positionTbl.put(c, PositionDirectionEnum.PD_Short, new AccountPosition(md.getName(), c, PositionDirectionEnum.PD_Short, accountRepo, shortTrades));
			
			md.getBindedAccounts().forEach(accountId -> {
				TradeGateway tradeGateway = (TradeGateway) gatewayMgr.get(Identifier.of(accountId));
				if(c.gatewayId().equals(tradeGateway.getDescription().getBindedMktGatewayId())) {
					contractAccountMap.put(c, accountMgr.get(Identifier.of(accountId)));
				}
			});
		});
		
		List<DealRecord> dealRecords = accountRepo.findDealRecords(md.getName());
		double accRealizedProfit = dealRecords.stream().mapToDouble(DealRecord::getDealProfit).sum();
		double accCommission = dealRecords.stream().mapToDouble(DealRecord::getTransactionFee).sum();
		
		return new ModuleAccount(md.getName(), md.getInitBalance(), accRealizedProfit, accCommission, positionTbl, contractAccountMap);
	}
	
	private TradeModule loadModule(ModuleDescription md, ModuleRuntimeDescription mrd) throws Exception {
		int weeksOfDataForPreparation = md.getWeeksOfDataForPreparation();
		LocalDate date = LocalDate.now().minusWeeks(weeksOfDataForPreparation);

		StrategyMetaInfo strategyComponent = md.getStrategySetting();
		TradeStrategy strategy = resolveComponent(strategyComponent);
		Assert.isTrue(strategy.type() == md.getType(), () -> String.format("该策略只能用于类型为[%s]的模组", strategy.type()));
		strategy.setStoreObject(mrd.getStoreObject());
		
		ModuleContext moduleCtx = null;
		if(md.getUsage() == ModuleUsage.PLAYBACK) {
			mrd = ModuleRuntimeDescription.builder()
					.moduleName(md.getName())
					.storeObject(new JSONObject())
					.build();
			moduleCtx = new PlaybackModuleContext(strategy, md, makeAccount(md, mrd), mktCenter, moduleRepo);
		} else {
			if(md.getType() == ModuleType.ARBITRAGE) {
				moduleCtx = new ArbitrageModuleContext(strategy, md, makeAccount(md, mrd), mktCenter, moduleRepo);
			} else {
				moduleCtx = new ModuleContext(strategy, md, makeAccount(md, mrd), mktCenter, moduleRepo);
			}
		}
		strategy.setContext(moduleCtx);
		log.info("模组[{}] 初始化数据起始计算日为：{}", md.getName(), date);
		final IModuleContext mctx = moduleCtx;
		if(md.getUsage() != ModuleUsage.PLAYBACK && weeksOfDataForPreparation > 0) {
			// 只有在非回测状态下，才需要预热数据
			for(ContractSimpleInfo csi : md.getBindedContracts()) {
				IContract c = mktCenter.getContract(Identifier.of(csi.getId()));
				IDataSource dataSrc = c.dataSource();
				Assert.notNull(dataSrc, () -> String.format("合约 [%s] 缺少数据源配置，无法加载历史数据", c.name()));
				if(c instanceof OptionChainContract) {
					// 对于期权链合约，要加载的是成员合约
					c.memberContracts().parallelStream().forEach(rc -> loadDataForInit(rc, weeksOfDataForPreparation, mctx));
				} else {
					loadDataForInit(c, weeksOfDataForPreparation, mctx);
				}
			}
		}
		return new TradeModule(moduleCtx);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends DynamicParamsAware> T resolveComponent(StrategyMetaInfo metaInfo) throws Exception {
		String clzName = metaInfo.getClassName();
		String paramClzName = clzName + "$InitParams";
		Class<?> type = Class.forName(clzName);
		Class<?> paramType = Class.forName(paramClzName);
		DynamicParamsAware obj = (DynamicParamsAware) type.getDeclaredConstructor().newInstance();
		DynamicParams paramObj = (DynamicParams) paramType.getDeclaredConstructor().newInstance();
		paramObj.resolveFromSource(metaInfo.getSettings());
		obj.initWithParams(paramObj);
		return (T) obj;
	}
	
	private void loadDataForInit(IContract c, int weeks, IModuleContext mctx) {
		KlineGenerator kg = new KlineGenerator(c.contract(), mctx::onBar);
		ModuleDataLoader loader = new ModuleDataLoader(c, kg);
		loader.load(weeks);
		
		// 本地仅加载最近的数据
		List<Bar> data = mdRepo.loadBars(c, LocalDate.now(), LocalDate.now().plusDays(3));
		for(Bar bar : data) {
			kg.updateBar(bar);
		}
	}
	
	public List<ModuleVO> getModules() {
		return moduleRepo.findAllSettings().stream()
				.map(md -> {
					IModule m = moduleMgr.get(Identifier.of(md.getName()));
					return ModuleVO.builder()
							.description(md)
							.enabled(m != null && m.getModuleContext().isEnabled())
							.state(m == null ? ModuleState.ERROR : m.getModuleContext().getState())
							.build();
				})
				.toList();
	}
	
	@Transactional
	public void removeModule(String name) {
		log.info("删除模组 [{}] ", name);
		unloadModule(name);
		moduleRepo.deleteRuntimeByName(name);
		accountRepo.removeDealRecords(name);
		accountRepo.removeHoldingTrades(name);
		sseController.sendModuleStatus();
		log.info("模组 [{}] 已删除", name);
	}
	
	public void enableModule(String moduleName) {
		log.info("启用模组 [{}] ", moduleName);
		IModule m = moduleMgr.get(Identifier.of(moduleName));
		if(m == null) {
			throw new NoSuchModuleException(moduleName);
		}
		m.setEnabled(true);
		sseController.sendModuleStatus();
	}
	
	public void disableModule(String moduleName) {
		log.info("停用模组 [{}] ", moduleName);
		IModule m = moduleMgr.get(Identifier.of(moduleName));
		if(m == null) {
			throw new NoSuchModuleException(moduleName);
		}
		m.setEnabled(false);
		sseController.sendModuleStatus();
	}

	@Transactional
	public void backtest(String moduleName, int numOfMonthToTest) throws Exception {
		log.info("回测模组 [{}]，回测时长{}个月", moduleName, numOfMonthToTest);
		log.info("回测前，会自动重置模组");
		updateModule(moduleMgr.get(Identifier.of(moduleName)).getModuleContext().getModuleDescription(), true);
		
		IModule m = moduleMgr.get(Identifier.of(moduleName));
		if(m == null) {
			throw new NoSuchModuleException(moduleName);
		}
		ModuleDescription md = m.getModuleContext().getModuleDescription();
		ModuleUsage usage = md.getUsage();
		if(usage != ModuleUsage.PLAYBACK) {
			log.error("该模组用途为{}，不能进行回测", usage);
			throw new IllegalStateException();
		}
		if(m.getModuleContext().bindedContracts().size() != 1) {
			throw new OperationException("error.backtest_supports_module_with_single_contract");
		}
		
		Contract c = m.getModuleContext().bindedContracts().getFirst();
		// 计算回测时段与预热时段
		LocalDate endDate = LocalDate.now();
		LocalDate startDate = endDate.minusMonths(numOfMonthToTest);
		LocalDate preloadStartDate = startDate.minusWeeks(md.getWeeksOfDataForPreparation());
		
		AtomicReference<LocalDate> yearMonth = new AtomicReference<>(preloadStartDate); 
		IModuleContext ctx = m.getModuleContext();
		c.contractDefinition().dataSource().getData(c, Interval.fromAlias(md.getKlineInterval()), preloadStartDate, endDate)
			.reversed().stream().forEach(bar -> {
				if(!bar.tradingDay().isBefore(startDate) && !ctx.isEnabled()) {
					ctx.setEnabled(true);
				}
				if(!yearMonth.get().isAfter(bar.tradingDay())) {
					yearMonth.set(yearMonth.get().plusWeeks(1));
					log.info("当前回测至：{}", bar.tradingDay());
				}
				
				ctx.onTick(Tick.builder()
						.gatewayId(bar.gatewayId())
						.contract(c)
						.tradingDay(bar.tradingDay())
						.actionDay(bar.actionDay())
						.actionTime(bar.actionTime())
						.actionTimestamp(bar.actionTimestamp())
						.channelType(bar.channelType())
						.preClosePrice(bar.preClosePrice())
						.preOpenInterest(bar.preOpenInterest())
						.preSettlePrice(bar.preSettlePrice())
						.volume(bar.volume())
						.volumeDelta(bar.volumeDelta())
						.openInterest(bar.openInterest())
						.openInterestDelta(bar.openInterestDelta())
						.turnover(bar.turnover())
						.turnoverDelta(bar.turnoverDelta())
						.lastPrice(bar.closePrice())
						.bidPrice(List.of(bar.closePrice()))
						.askPrice(List.of(bar.closePrice()))
						.build());
				ctx.onBar(bar);
			});
		log.info("回测结束");
		ctx.setEnabled(false);
	}

	public void mockTrade(ModuleMockTradeDTO trade) {
		String name = trade.getModuleName();
		log.info("调整模组 [{}] 持仓", name);
		IModule m = moduleMgr.get(Identifier.of(name));
		m.getModuleContext().onTrade(Trade.builder()
				.originOrderId(Constants.MOCK_ORDER_ID)
				.contract(mktCenter.getContract(Identifier.of(trade.getItem().getId())).contract())
				.direction(trade.getDirection())
				.offsetFlag(trade.getOffsetFlag())
				.volume(trade.getVolume())
				.price(trade.getPrice())
				.tradingDay(LocalDate.now())
				.tradeTime(LocalTime.now())
				.tradeDate(LocalDate.now())
				.tradeTimestamp(System.currentTimeMillis())
				.build());
	}
	
	public List<ModuleDealRecordVO> getDealRecords(String moduleName) {
		return accountRepo.findDealRecords(moduleName)
				.stream()
				.map(deal -> {
					TradeField openTrade = deal.convertOpenTrade();
					TradeField closeTrade = deal.convertCloseTrade();
					int factor = openTrade.getDirection() == DirectionEnum.D_Buy ? 1 : -1;
					double vol = Math.min(closeTrade.getVolume(), openTrade.getVolume());
					int profit = (int) (factor * (closeTrade.getPrice() - openTrade.getPrice()) * vol * closeTrade.getContract().getMultiplier());
					return ModuleDealRecordVO.builder()
							.name(openTrade.getContract().getName())
							.symbol(openTrade.getContract().getSymbol())
							.dir(OrderUtils.getClosingDirection(closeTrade.getDirection()))
							.openPrice(BigDecimal.valueOf(openTrade.getPrice()).setScale(openTrade.getContract().getPricePrecision(), RoundingMode.HALF_UP) + "")
							.closePrice(BigDecimal.valueOf(closeTrade.getPrice()).setScale(closeTrade.getContract().getPricePrecision(), RoundingMode.HALF_UP) + "")
							.vol(vol)
							.profit(profit)
							.transactionFee((int)deal.getTransactionFee())
							.openTimestamp(openTrade.getTradeTimestamp())
							.closeTimestamp(closeTrade.getTradeTimestamp())
							.contractId(closeTrade.getContract().getContractId())
							.build();
				})
				.filter(Objects::nonNull)
				.sorted((a,b) ->  a.getCloseTimestamp() > b.getCloseTimestamp() ? -1 : 1)
				.toList();
	}

	public ModuleChartDescription getChart(String moduleName) {
		IModule m = moduleMgr.get(Identifier.of(moduleName));
		if(m == null) {
			throw new NoSuchModuleException(moduleName);
		}
		
		return m.getModuleContext().getChartDescription();
	}

	public ModuleRuntimeVO getRuntime(String moduleName) {
		IModule m = moduleMgr.get(Identifier.of(moduleName));
		if(m == null) {
			throw new NoSuchModuleException(moduleName);
		}
		
		ModuleAccount account = (ModuleAccount) m.getModuleAccount();
		double initBalance = m.getModuleContext().getModuleDescription().getInitBalance();
		double balance = account.accountBalance();
		double available = account.availableBalance();
		double marginUtilized = balance - available;
		double commission = account.commission();
		double realizedPL = account.realizedProfit();
		
		List<PositionVO> positions = account.getPositions()
				.stream()
				.filter(p -> p.position() > 0)
				.map(p -> PositionVO.builder()
						.contractId(p.contract().contractId())
						.name(p.contract().name())
						.symbol(p.contract().symbol())
						.dir(p.positionDirection())
						.total(p.position())
						.available(p.position() - p.frozen())
						.openPrice(BigDecimal.valueOf(p.openPrice()).setScale(p.contract().pricePrecision(), RoundingMode.HALF_UP).toPlainString())
						.lastPrice(p.lastTick() != null 
							? BigDecimal.valueOf(p.lastTick().lastPrice()).setScale(p.contract().pricePrecision(), RoundingMode.HALF_UP).toPlainString() 
							: "0")
						.profit(p.positionProfit())
						.gatewayId(p.gatewayId())
						.closingDir(OrderUtils.getClosingDirection(p.positionDirection()))
						.build())
				.toList();
		double unrealizedProfit = positions.stream().mapToDouble(PositionVO::getProfit).sum();
		double netPL = realizedPL + unrealizedProfit - commission;
		List<ModuleDealRecordVO> dealRecords = getDealRecords(moduleName);
		int totalWins = dealRecords.stream().filter(rec -> rec.getProfit() > 0).toList().size();
		
		double avgProfit = dealRecords.stream().filter(rec -> rec.getProfit() > 0).mapToDouble(rec -> rec.getProfit()).average().orElse(0.1D);
		double avgLoss = dealRecords.stream().filter(rec -> rec.getProfit() < 0).mapToDouble(rec -> Math.abs(rec.getProfit())).average().orElse(0.1D);
		
		double maxDrawdown = 0;
		double peak = initBalance;
		if(!dealRecords.isEmpty()) {
			List<ModuleDealRecordVO> ascRecords = dealRecords.reversed();
			double cumulative = 0;
			for(ModuleDealRecordVO rec : ascRecords) {
				cumulative += rec.getProfit() - rec.getTransactionFee();
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
		double currentEquity = initBalance + netPL;
		if(currentEquity > peak) {
			peak = currentEquity;
		}
		double currentDD = peak - currentEquity;
		if(currentDD > maxDrawdown) {
			maxDrawdown = currentDD;
		}
		double maxDrawdownRate = peak > 0 ? (maxDrawdown / peak) * 100 : 0;
		
		Double annualizedReturnRate = 0D;
		if(!dealRecords.isEmpty()) {
			LocalDate firstDate = CommonUtils.millsToLocalDateTime(dealRecords.getLast().getOpenTimestamp()).toLocalDate();
			LocalDate lastDate = CommonUtils.millsToLocalDateTime(dealRecords.getFirst().getCloseTimestamp()).toLocalDate();
			double years = Period.between(firstDate, lastDate).getDays() / 365.0;
			annualizedReturnRate = (netPL / initBalance) / years * 100.0;
		}
		
		return ModuleRuntimeVO.builder()
				.name(moduleName)
				.accountRt(ModuleAccountRuntimeVO.builder()
						.initBalance(initBalance)
						.available(available)
						.marginUtilized(marginUtilized)
						.balance(balance)
						.realizedPL(realizedPL)
						.unrealizedPL(unrealizedProfit)
						.totalFees(commission)
						.netPL(netPL)
						.maxDrawdown(maxDrawdown)
						.maxDrawdownRate(maxDrawdownRate)
						.annualizedReturnRate(annualizedReturnRate)
						.winRate(100.0 * totalWins / dealRecords.size())
						.profitLossRatio(avgProfit / avgLoss)
						.modulePositions(positions)
						.build())
				.build();
	}

	@Transactional
	@Override
	public void run(String... args) throws Exception {
		log.info("开始加载模组");
		for(ModuleDescription md : moduleRepo.findAllSettings()) {
			// 自动重置回测模组的交易记录
			if(md.getUsage() == ModuleUsage.PLAYBACK) {
				accountRepo.removeDealRecords(md.getName());
			}
			try {
				ModuleRuntimeDescription mrd = moduleRepo.findRuntimeByName(md.getName());
				TradeModule module = loadModule(md, mrd);
				moduleMgr.add(Identifier.of(md.getName()), module);
			} catch (Exception e) {
				log.error("模组 [{}] 加载失败。原因：{}", md.getName(), e.getMessage());
				log.warn("模组将被自动移除");
				removeModule(md.getName());
			}
		}
		log.info("模组加载完毕");
	}

}
