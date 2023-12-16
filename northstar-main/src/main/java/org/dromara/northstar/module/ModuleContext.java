package org.dromara.northstar.module;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.AccountRuntimeDescription;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModulePositionDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.common.model.Tuple;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.Position;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.IndicatorValueUpdateHelper;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleAccount;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.OrderRequestFilter;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.support.log.ModuleLoggerFactory;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;
import org.slf4j.Logger;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.protobuf.InvalidProtocolBufferException;

import cn.hutool.core.lang.Assert;
import lombok.Getter;
import lombok.Setter;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.TradeField;

public class ModuleContext implements IModuleContext{
	
	@Getter
	@Setter
	protected IModule module;
	
	protected Logger logger;
	
	protected TradeStrategy tradeStrategy;
	
	protected IModuleRepository moduleRepo;
	
	protected ModuleAccount moduleAccount;
	
	/* originOrderId -> orderReq */
	protected ConcurrentMap<String, SubmitOrderReq> orderReqMap = new ConcurrentHashMap<>();
	
	/* unifiedSymbol -> contract */
	protected ConcurrentMap<String, Contract> contractMap = new ConcurrentHashMap<>();
	
	protected ConcurrentMap<Contract, Tick> latestTickMap = new ConcurrentHashMap<>();
	
	protected ConcurrentMap<Contract, Queue<Bar>> barBufQMap = new ConcurrentHashMap<>();
	
	protected ConcurrentMap<Contract, Long> barFilterMap = new ConcurrentHashMap<>();
	
	/* indicator -> values */
	protected ConcurrentMap<Indicator, Queue<TimeSeriesValue>> indicatorValBufQMap = new ConcurrentHashMap<>(); 
	
	/* contract -> indicatorName -> indicator */
	protected Table<Contract, String, Indicator> indicatorNameTbl = HashBasedTable.create();
	
	protected Set<IndicatorValueUpdateHelper> indicatorHelperSet = new HashSet<>();
	
	protected ConcurrentMap<Contract, TradeIntent> tradeIntentMap = new ConcurrentHashMap<>();	// 交易意图
	
	protected final AtomicInteger bufSize = new AtomicInteger(0);
	
	protected final BarMergerRegistry registry;
	
	protected boolean enabled;
	
	protected LocalDate tradingDay;
	
	protected IContractManager contractMgr;
	
	protected OrderRequestFilter orderReqFilter;
	
	public ModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRtDescription,
			IContractManager contractMgr, IModuleRepository moduleRepo, ModuleLoggerFactory loggerFactory, BarMergerRegistry barMergerRegistry) {
		this.tradeStrategy = tradeStrategy;
		this.moduleRepo = moduleRepo;
		this.contractMgr = contractMgr;
		this.registry = barMergerRegistry;
		this.logger = loggerFactory.getLogger(moduleDescription.getModuleName());
		this.bufSize.set(moduleDescription.getModuleCacheDataSize());
		this.moduleAccount = new ModuleAccount(moduleDescription, moduleRtDescription, new ModuleStateMachine(this), moduleRepo, contractMgr, logger);
		this.orderReqFilter = new DefaultOrderFilter(moduleDescription.getModuleAccountSettingsDescription().stream().flatMap(mad -> mad.getBindedContracts().stream()).toList(), this);
		moduleDescription.getModuleAccountSettingsDescription().stream()
			.forEach(mad -> {
				for(ContractSimpleInfo csi : mad.getBindedContracts()) {
					IContract contract = contractMgr.getContract(Identifier.of(csi.getValue()));
					Contract cf = contract.contract();
					contractMap.put(cf.unifiedSymbol(), cf);
					barBufQMap.put(cf, new ConcurrentLinkedQueue<>());
					registry.addListener(cf, moduleDescription.getNumOfMinPerBar(), PeriodUnit.MINUTE, tradeStrategy);
					registry.addListener(cf, moduleDescription.getNumOfMinPerBar(), PeriodUnit.MINUTE, this);
				}
			});
	}
	
	@Override
	public boolean explain(boolean expression, String infoMessage, Object... args) {
		if(expression) {
			getLogger().info(infoMessage, args);
		}
		return expression;
	}

	@Override
	public Contract getContract(String unifiedSymbol) {
		if(!contractMap.containsKey(unifiedSymbol)) {
			throw new NoSuchElementException("模组没有绑定合约：" + unifiedSymbol);
		}
		return contractMap.get(unifiedSymbol);
	}

	@Override
	public void submitOrderReq(TradeIntent tradeIntent) {
		if(!module.isEnabled()) {
			if(isReady()) {
				getLogger().info("策略处于停用状态，忽略委托单");
			}
			return;
		}
		Tick tick = latestTickMap.get(tradeIntent.getContract());
		if(Objects.isNull(tick)) {
			getLogger().warn("没有TICK行情数据时，忽略下单请求");
			return;
		}
		getLogger().info("收到下单意图：{}", tradeIntent);
		tradeIntentMap.put(tradeIntent.getContract(), tradeIntent);
		tradeIntent.setContext(this);
        tradeIntent.onTick(tick);
	}

	@Override
	public int numOfMinPerMergedBar() {
		return module.getModuleDescription().getNumOfMinPerBar();
	}

	@Override
	public IAccount getAccount(Contract contract) {
		return module.getAccount(contract);
	}

	@Override
	public IModuleAccount getModuleAccount() {
		return moduleAccount;
	}

	@Override
	public ModuleState getState() {
		return moduleAccount.getModuleState();
	}

	@Override
	public void disabledModule() {
		getLogger().warn("策略层主动停用模组");
		setEnabled(false);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public void registerIndicator(Indicator indicator) {
		checkIndicator(indicator);
		Configuration cfg = indicator.getConfiguration();
		IndicatorValueUpdateHelper helper = new IndicatorValueUpdateHelper(indicator);
		indicatorHelperSet.add(helper);
		registry.addListener(cfg.contract(), cfg.numOfUnits(), cfg.period(), helper);
	}
	
	public void checkIndicator(Indicator indicator) {
		// 递归子指标
		for(Indicator in : indicator.dependencies()) {
			checkIndicator(in);
		}
		Configuration cfg = indicator.getConfiguration();
		String indicatorName = String.format("%s_%d%s", cfg.indicatorName(), cfg.numOfUnits(), cfg.period().symbol());
		logger.trace("检查指标配置信息：{}", indicatorName);
		Assert.isTrue(cfg.numOfUnits() > 0, "周期数必须大于0，当前为：" + cfg.numOfUnits());
		Assert.isTrue(cfg.cacheLength() > 0, "指标回溯长度必须大于0，当前为：" + cfg.cacheLength());
		if(cfg.visible()) {		// 不显示的指标可以不做重名校验
			Assert.isTrue(!indicatorNameTbl.contains(cfg.contract(), indicatorName) || indicator.equals(indicatorNameTbl.get(cfg.contract(), indicatorName)), "指标 [{} -> {}] 已存在。不能重名", cfg.contract().unifiedSymbol(), indicatorName);
			indicatorNameTbl.put(cfg.contract(), indicatorName, indicator);
		}
		indicatorValBufQMap.put(indicator, new ConcurrentLinkedDeque<>());
	}
	
	@Override
	public void onTick(Tick tick) {
		getLogger().trace("TICK信息: {} {} {} {}，最新价：{}，累计成交：{}，成交量：{}，累计持仓：{}，持仓量：{}", 
				tick.contract().unifiedSymbol(), tick.actionDay(), tick.actionTime(), tick.actionTimestamp(),
				tick.lastPrice(), tick.volume(), tick.volumeDelta(), tick.openInterest(), tick.openInterestDelta());
		if(tradeIntentMap.containsKey(tick.contract())) {
			TradeIntent tradeIntent = tradeIntentMap.get(tick.contract());
			tradeIntent.onTick(tick);
			if(tradeIntent.hasTerminated()) {
				tradeIntentMap.remove(tick.contract());
				getLogger().debug("移除交易意图：{}", tick.contract().unifiedSymbol());
			}
		}
		if(!Objects.equals(tick.tradingDay(), tradingDay)) {
			tradingDay = tick.tradingDay();
		}
		indicatorHelperSet.forEach(helper -> helper.onTick(tick));
		moduleAccount.onTick(tick);
		latestTickMap.put(tick.contract(), tick);
		tradeStrategy.onTick(tick);
	}

	@Override
	public void onBar(Bar bar) {
		if(barFilterMap.containsKey(bar.contract()) && barFilterMap.get(bar.contract()) >= bar.actionTimestamp()) {
			//过滤掉可能存在的重复数据
			return;
		}
		getLogger().trace("分钟Bar信息: {} {} {} {}，最新价: {}，成交量：{}，累计持仓：{}，持仓量：{}", bar.contract().unifiedSymbol(), bar.actionDay(), bar.actionTime(), bar.actionTimestamp(),
				bar.closePrice(), bar.volume(), bar.openInterest(), bar.openInterestDelta());
		barFilterMap.put(bar.contract(), bar.actionTimestamp());
		indicatorHelperSet.forEach(helper -> helper.onBar(bar));
		registry.onBar(bar);		
	}
	
	@Override
	public void onMergedBar(Bar bar) {
		getLogger().debug("合并Bar信息: {} {} {} {}，最新价: {}", bar.contract().unifiedSymbol(), bar.actionDay(), bar.actionTime(), bar.actionTimestamp(), bar.closePrice());
		try {			
			indicatorHelperSet.stream().map(IndicatorValueUpdateHelper::getIndicator).forEach(indicator -> visualize(indicator, bar));
		} catch(Exception e) {
			getLogger().error(e.getMessage(), e);
		}
		if(barBufQMap.get(bar.contract()).size() >= bufSize.intValue()) {
			barBufQMap.get(bar.contract()).poll();
		}
		barBufQMap.get(bar.contract()).offer(bar);
		if(isEnabled()) {
			moduleRepo.saveRuntime(getRuntimeDescription(false));
		}
	}
	
	private void visualize(Indicator indicator, Bar bar) {
		for(Indicator in : indicator.dependencies()) {
			visualize(in, bar);
		}
		if(!indicator.getConfiguration().contract().equals(bar.contract())) {
			return;
		}
		ConcurrentLinkedDeque<TimeSeriesValue> list = (ConcurrentLinkedDeque<TimeSeriesValue>) indicatorValBufQMap.get(indicator);
		if(list.size() >= bufSize.intValue()) {
			list.poll();
		}
		if(indicator.isReady() && indicator.getConfiguration().visible() && indicator.get(0).timestamp() == bar.actionTimestamp()
				&& (list.isEmpty() || list.peekLast().getTimestamp() != bar.actionTimestamp())
				&& (isEndOfTheTradingDay(bar) || indicator.getConfiguration().ifPlotPerBar() || !indicator.get(0).unstable())) {
			list.offer(new TimeSeriesValue(indicator.get(0).value(), bar.actionTimestamp()));
		}
	}
	
	private boolean isEndOfTheTradingDay(Bar bar) {
		TradeTimeDefinition timeDef = bar.contract().contractDefinition().tradeTimeDef();
		List<TimeSlot> times = timeDef.timeSlots();
		LocalTime t = times.get(times.size() - 1).end();
		return t.equals(bar.actionTime());
	}

	@Override
	public void onOrder(Order order) {
		if(!orderReqMap.containsKey(order.originOrderId())) {
			return;
		}
		getLogger().info("收到订单反馈：{} 合约：{} 订单状态：{} {}", order.originOrderId(), order.contract().name(), order.orderStatus(), order.statusMsg());
		if(!OrderUtils.isValidOrder(order) || OrderUtils.isDoneOrder(order)) {
			// 延时3秒再移除订单信息，避免移除了订单信息后，成交无法匹配的问题
			CompletableFuture.runAsync(() -> orderReqMap.remove(order.originOrderId()), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS));
		}
		moduleAccount.onOrder(order);
		tradeStrategy.onOrder(order);
		if(tradeIntentMap.containsKey(order.contract())) {
			TradeIntent tradeIntent = tradeIntentMap.get(order.contract());
			tradeIntent.onOrder(order);
		}
	}

	@Override
	public void onTrade(Trade trade) {
		if(!orderReqMap.containsKey(trade.originOrderId()) && !StringUtils.equals(trade.originOrderId(), Constants.MOCK_ORDER_ID)) {
			return;
		} 
		if(getLogger().isInfoEnabled()) {
			getLogger().info("收到成交反馈：{}， 操作：{}{}，合约：{}，价格：{}， 手数：{}", trade.originOrderId(), FieldUtils.chn(trade.direction()),
					FieldUtils.chn(trade.offsetFlag()), trade.contract().name(), trade.price(), trade.volume());
		}
		moduleAccount.onTrade(trade);
		tradeStrategy.onTrade(trade);
		moduleRepo.saveRuntime(getRuntimeDescription(false));
		
		if(tradeIntentMap.containsKey(trade.contract())) {
			TradeIntent tradeIntent = tradeIntentMap.get(trade.contract());
			tradeIntent.onTrade(trade);
			if(tradeIntent.hasTerminated()) {
				tradeIntentMap.remove(trade.contract());
			}
		}
	}

	@Override
	public void initData(List<Bar> barData) {
		if(barData.isEmpty()) {
			getLogger().debug("初始化数据为空");
			return;
		}
		
		getLogger().debug("合约{} 初始化数据 {} {} -> {} {}", barData.get(0).contract().unifiedSymbol(),
				barData.get(0).actionDay(), barData.get(0).actionTime(),
				barData.get(barData.size() - 1).actionDay(), barData.get(barData.size() - 1).actionTime());
		for(Bar bar : barData) {
			onBar(bar);
		}
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
		ModulePositionDescription posDescription = ModulePositionDescription.builder()
				.logicalPositions(moduleAccount.getPositions().stream().map(p -> p.toPositionField().toByteArray()).toList())
				.nonclosedTrades(moduleAccount.getNonclosedTrades().stream().map(t -> t.toTradeField().toByteArray()).toList())
				.build();
		ModuleAccountRuntimeDescription accRtDescription = ModuleAccountRuntimeDescription.builder()
				.initBalance(moduleAccount.getInitBalance())
				.accCloseProfit(moduleAccount.getAccCloseProfit())
				.accDealVolume(moduleAccount.getAccDealVolume())
				.accCommission(moduleAccount.getAccCommission())
				.maxDrawback(moduleAccount.getMaxDrawback())
				.maxDrawbackPercentage(moduleAccount.getMaxDrawbackPercentage())
				.maxProfit(moduleAccount.getMaxProfit())
				.positionDescription(posDescription)
				.availableAmount(moduleAccount.availableAmount())
				.build();
		List<AccountRuntimeDescription> accRts = contractMap.values().stream()
				.map(this::getAccount)
				.collect(Collectors.toSet())
				.stream()
				.map(acc -> AccountRuntimeDescription.builder()
						.name(acc.accountId())
						.balance(acc.accountBalance())
						.availableAmount(acc.availableAmount())
						.build())
				.toList();
		ModuleRuntimeDescription mad = ModuleRuntimeDescription.builder()
				.moduleName(module.getName())
				.enabled(module.isEnabled())
				.moduleState(moduleAccount.getModuleState())
				.storeObject(tradeStrategy.getStoreObject())
				.strategyInfos(tradeStrategy.strategyInfos())
				.moduleAccountRuntime(accRtDescription)
				.accountRuntimes(accRts)
				.build();
		
		if(fullDescription) {
			List<ModuleDealRecord> dealRecords = moduleRepo.findAllDealRecords(module.getName());
			double avgProfit = dealRecords.stream().mapToDouble(ModuleDealRecord::getDealProfit).average().orElse(0D);
			double annualizedRateOfReturn = 0;
			if(!dealRecords.isEmpty()) {
				LocalDate startDate = LocalDate.parse(parse(dealRecords.get(0).getOpenTrade()).getTradeDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
				LocalDate endDate = LocalDate.parse(parse(dealRecords.get(dealRecords.size() - 1).getCloseTrade()).getTradeDate(), DateTimeConstant.D_FORMAT_INT_FORMATTER);
				long days = ChronoUnit.DAYS.between(startDate, endDate);
				double totalEarning = moduleAccount.getAccCloseProfit() - moduleAccount.getAccCommission();
				annualizedRateOfReturn = (totalEarning / moduleAccount.getInitBalance()) / days * 365;  
			}
			accRtDescription.setAvgEarning(avgProfit);
			accRtDescription.setAnnualizedRateOfReturn(annualizedRateOfReturn);
			
			Map<String, List<String>> indicatorMap = new HashMap<>();
			Map<Contract, LinkedHashMap<Long, JSONObject>> symbolTimeObject = new HashMap<>();
			barBufQMap.entrySet().forEach(e -> 
				e.getValue().forEach(bar -> {
					symbolTimeObject.putIfAbsent(bar.contract(), new LinkedHashMap<>());
					symbolTimeObject.get(bar.contract()).put(bar.actionTimestamp(), assignBar(bar));
				})
			);
			
			indicatorValBufQMap.entrySet().forEach(e -> {
				Indicator in = e.getKey();
				Configuration cfg = in.getConfiguration();
				String indicatorName = String.format("%s_%d%s", cfg.indicatorName(), cfg.numOfUnits(), cfg.period().symbol());
				indicatorMap.putIfAbsent(cfg.contract().name(), new ArrayList<>());
				if(cfg.visible()) {
					indicatorMap.get(cfg.contract().name()).add(indicatorName);
				}
				Collections.sort(indicatorMap.get(cfg.contract().name()));
				
				e.getValue().forEach(tv -> {
					if(!symbolTimeObject.containsKey(cfg.contract())
							|| !symbolTimeObject.get(cfg.contract()).containsKey(tv.getTimestamp())) {
						return;
					}
					symbolTimeObject.get(cfg.contract()).get(tv.getTimestamp()).put(indicatorName, tv.getValue());
				});
			});
			Map<String, JSONArray> dataMap = barBufQMap.entrySet().stream().collect(Collectors.toMap(
					e -> e.getKey().name(),
					e -> {
						if(!symbolTimeObject.containsKey(e.getKey())) 							
							return new JSONArray();
						return new JSONArray(symbolTimeObject.get(e.getKey()).values().stream().toList());
					})
			);
			
			mad.setIndicatorMap(indicatorMap);
			mad.setDataMap(dataMap);
		}
		return mad;
	}
	
	private TradeField parse(byte[] data) {
		try {
			return TradeField.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private JSONObject assignBar(Bar bar) {
		JSONObject json = new JSONObject();
		json.put("open", bar.openPrice());
		json.put("low", bar.lowPrice());
		json.put("high", bar.highPrice());
		json.put("close", bar.closePrice());
		json.put("volume", bar.volumeDelta());
		json.put("openInterestDelta", bar.openInterestDelta());
		json.put("openInterest", bar.openInterest());
		json.put("timestamp", bar.actionTimestamp());
		return json;
	}

	@Override
	public Optional<String> submitOrderReq(Contract contract, SignalOperation operation, PriceType priceType, int volume, double price) {
		if(!module.isEnabled()) {
			if(isReady()) {
				getLogger().info("策略处于停用状态，忽略委托单");
			}
			return Optional.empty();
		}
		Tick tick = latestTickMap.get(contract);
		Assert.notNull(tick, "没有行情时不应该发送订单");
		Assert.isTrue(volume > 0, "下单手数应该为正数。当前为" + volume);
		
		double orderPrice = priceType.resolvePrice(tick, operation, price);
		if(getLogger().isInfoEnabled()) {
			getLogger().info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
					tick.actionDay(), tick.actionTime(),
					contract.unifiedSymbol(), operation.text(), orderPrice, volume, priceType);
		}
		String id = UUID.randomUUID().toString();
		String gatewayId = getAccount(contract).accountId();
		DirectionEnum direction = OrderUtils.resolveDirection(operation);
		int factor = FieldUtils.directionFactor(direction);
		double plusPrice = module.getModuleDescription().getOrderPlusTick() * contract.priceTick(); // 超价设置
		Position pos = getAccount(contract).getPosition(OrderUtils.getClosingDirection(direction), contract)
				.orElse(Position.builder().contract(contract).build());
		Tuple<OffsetFlagEnum, Integer> tuple = module.getModuleDescription().getClosingPolicy().resolve(operation, pos, volume);
		if(tuple.t1() == OffsetFlagEnum.OF_CloseToday) {
			Position updatePos = pos.toBuilder().tdFrozen(tuple.t2()).build();
			getAccount(contract).onPosition(updatePos);
		} else if(tuple.t1() == OffsetFlagEnum.OF_CloseYesterday) {
			Position updatePos = pos.toBuilder().ydFrozen(tuple.t2()).build();
			getAccount(contract).onPosition(updatePos);
		}
		return Optional.ofNullable(submitOrderReq(SubmitOrderReq.builder()
				.originOrderId(id)
				.contract(contract)
				.gatewayId(gatewayId)
				.direction(direction)
				.offsetFlag(tuple.t1())
				.volume(tuple.t2())
				.currency(contract.currency())
				.price(orderPrice + factor * plusPrice)	// 自动加上超价
				.volumeCondition(VolumeConditionEnum.VC_AV)
				.timeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.orderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.contingentCondition(ContingentConditionEnum.CC_Immediately)
				.actionTimestamp(System.currentTimeMillis())
				.minVolume(1)
				.build()));
	}
	
	private String submitOrderReq(SubmitOrderReq orderReq) {
		if(getLogger().isInfoEnabled()) {			
			getLogger().info("发单：{}，{}", orderReq.originOrderId(), LocalDateTime.ofInstant(Instant.ofEpochMilli(orderReq.actionTimestamp()), ZoneId.systemDefault()));
		}
		try {
			moduleAccount.onSubmitOrder(orderReq);
		} catch (InsufficientException e) {
			getLogger().error("发单失败。原因：{}", e.getMessage());
			tradeIntentMap.remove(orderReq.contract());
			getLogger().warn("模组余额不足，主动停用模组");
			setEnabled(false);
			return null;
		}
		try {
			if(Objects.nonNull(orderReqFilter)) {
				orderReqFilter.doFilter(orderReq);
			}
		} catch (Exception e) {
			getLogger().error("发单失败。原因：{}", e.getMessage());
			tradeIntentMap.remove(orderReq.contract());
			return null;
		}
		Contract contract = orderReq.contract();
		String originOrderId = module.getAccount(contract).submitOrder(orderReq);
		orderReqMap.put(originOrderId, orderReq);
		return originOrderId;
	}

	@Override
	public boolean isOrderWaitTimeout(String originOrderId, long timeout) {
		if(!orderReqMap.containsKey(originOrderId)) {
			return false;
		}
		
		SubmitOrderReq orderReq = orderReqMap.get(originOrderId);
		return System.currentTimeMillis() - orderReq.actionTimestamp() > timeout;
	}

	@Override
	public void cancelOrder(String originOrderId) {
		if(!orderReqMap.containsKey(originOrderId)) {
			getLogger().debug("找不到订单：{}", originOrderId);
			return;
		}
		if(!getState().isOrdering()) {
			getLogger().info("非下单状态，忽略撤单请求：{}", originOrderId);
			return;
		}
		getLogger().info("撤单：{}", originOrderId);
		Contract contract = orderReqMap.get(originOrderId).contract();
		module.getAccount(contract).cancelOrder(originOrderId);
	}

	@Override
	public void setEnabled(boolean enabled) {
		getLogger().info("【{}】 模组", enabled ? "启用" : "停用");
		this.enabled = enabled;
		moduleRepo.saveRuntime(getRuntimeDescription(false));
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setOrderRequestFilter(OrderRequestFilter filter) {
		this.orderReqFilter = filter;
	}

	private boolean isReady;
	
	@Override
	public boolean isReady() {
		return isReady;
	}

	@Override
	public void onReady() {
		isReady = true;
	}

	@Override
	public int getDefaultVolume() {
		return module.getModuleDescription().getDefaultVolume();
	}

}
