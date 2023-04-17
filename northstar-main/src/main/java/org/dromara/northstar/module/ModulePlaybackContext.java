package org.dromara.northstar.module;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.exception.NoSuchElementException;
import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModulePositionDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.common.utils.BarUtils;
import org.dromara.northstar.common.utils.ContractUtils;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.gateway.api.TradeGateway;
import org.dromara.northstar.gateway.api.domain.contract.Contract;
import org.dromara.northstar.strategy.api.ClosingStrategy;
import org.dromara.northstar.strategy.api.IComboIndicator;
import org.dromara.northstar.strategy.api.IDisposablePriceListener;
import org.dromara.northstar.strategy.api.IModule;
import org.dromara.northstar.strategy.api.IModuleAccountStore;
import org.dromara.northstar.strategy.api.IModuleContext;
import org.dromara.northstar.strategy.api.IndicatorFactory;
import org.dromara.northstar.strategy.api.MergedBarListener;
import org.dromara.northstar.strategy.api.TradeStrategy;
import org.dromara.northstar.strategy.api.constant.DisposablePriceListenerType;
import org.dromara.northstar.strategy.api.constant.PriceType;
import org.dromara.northstar.strategy.api.indicator.Indicator;
import org.dromara.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import org.dromara.northstar.strategy.api.indicator.Indicator.Configuration;
import org.dromara.northstar.strategy.api.indicator.Indicator.PeriodUnit;
import org.dromara.northstar.strategy.api.indicator.Indicator.ValueType;
import org.dromara.northstar.strategy.api.log.ModuleLoggerFactory;
import org.dromara.northstar.strategy.api.utils.bar.BarMergerRegistry;
import org.dromara.northstar.strategy.api.utils.bar.BarMergerRegistry.ListenerType;
import org.dromara.northstar.strategy.api.utils.trade.DealCollector;
import org.dromara.northstar.strategy.api.utils.trade.DisposablePriceListener;
import org.dromara.northstar.strategy.api.utils.trade.TradeIntent;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.util.concurrent.AtomicDouble;

import cn.hutool.core.lang.Assert;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 回测模组上下文
 * 该上下文与实盘上下文的区别是不会往外发单，一切成交均为内部模拟
 * @author KevinHuangwl
 *
 */
public class ModulePlaybackContext implements IModuleContext, MergedBarListener {
	
	private static final ILoggerFactory logFactory = new ModuleLoggerFactory();

	public static final String PLAYBACK_GATEWAY = "回测账户";
	
	private String moduleName;
	
	private ClosingStrategy closingStrategy;

	private TradeStrategy tradeStrategy;
	
	private IModuleAccountStore accStore;
	
	private IModule module;
	
	private int numOfMinsPerBar;
	
	private DealCollector dealCollector;
	
	private Set<String> bindedSymbolSet = new HashSet<>();
	/* originOrderId -> orderReq */
	private Map<String, SubmitOrderReqField> orderReqMap = new HashMap<>();
	/* unifiedSymbol -> tick */
	private Map<String, TickField> latestTickMap = new HashMap<>();
	
	/* unifiedSymbol -> barQ */
	private Map<String, Queue<BarField>> barBufQMap = new HashMap<>();
	
	/* contract -> gateway */
	private Map<ContractField, TradeGateway> gatewayMap = new HashMap<>();
	
	/* indicator -> values */
	private Map<Indicator, Queue<TimeSeriesValue>> indicatorValBufQMap = new HashMap<>(); 
	
	private final AtomicInteger bufSize = new AtomicInteger(0);
	
	/* unifiedSymbol -> contract */
	private Map<String, ContractField> contractMap = new HashMap<>();
	private Map<ContractField, Contract> contractMap2 = new HashMap<>();
	
	private Set<DisposablePriceListener> listenerSet = new HashSet<>();
	
	private Logger mlog;
	
	private final IndicatorFactory indicatorFactory = new IndicatorFactory(this);
	
	private final IndicatorFactory inspectedValIndicatorFactory = new IndicatorFactory(this);
	
	private final HashSet<IComboIndicator> comboIndicators = new HashSet<>();
	
	private final BarMergerRegistry registry = new BarMergerRegistry();
	
	private Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback;
	
	private Consumer<ModuleDealRecord> onDealCallback;
	
	public ModulePlaybackContext(String name, TradeStrategy tradeStrategy, IModuleAccountStore accStore, int numOfMinsPerBar, 
			int bufSize, DealCollector dealCollector, Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback, Consumer<ModuleDealRecord> onDealCallback) {
		this.moduleName = name;
		this.mlog = logFactory.getLogger(name);
		this.tradeStrategy = tradeStrategy;
		this.accStore = accStore;
		this.bufSize.set(bufSize);
		this.numOfMinsPerBar = numOfMinsPerBar;
		this.closingStrategy = new PriorTodayClosingStrategy();
		this.onRuntimeChangeCallback = onRuntimeChangeCallback;
		this.onDealCallback = onDealCallback;
		this.dealCollector = dealCollector;
	}
	
	@Override
	public String getModuleName() {
		return moduleName;
	}

	@Override
	public boolean explain(boolean expression, String infoMessage, Object... args) {
		if(expression) {
			mlog.info(infoMessage, args);
		}
		return expression;
	}

	@Override
	public ContractField getContract(String unifiedSymbol) {
		if(!contractMap.containsKey(unifiedSymbol)) {
			throw new NoSuchElementException("找不到合约：" + unifiedSymbol);
		}
		return contractMap.get(unifiedSymbol);
	}

	// 所有的委托都会立马转为成交单
	@Override
	public synchronized Optional<String> submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume,
			double price) {
		if(!module.isEnabled()) {
			mlog.info("策略处于停用状态，忽略委托单");
			return Optional.empty();
		}
		TickField tick = latestTickMap.get(contract.getUnifiedSymbol());
		Assert.notNull(tick, "没有行情时不应该发送订单。请确保行情预热时，模组应处于【停用】状态");
		Assert.isTrue(volume > 0, "下单手数应该为正数");
		
		if(mlog.isInfoEnabled()) {			
			mlog.info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
					tick.getActionDay(), LocalTime.parse(tick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER),
					contract.getUnifiedSymbol(), operation.text(), price, volume, priceType);
		}
		String id = UUID.randomUUID().toString();
		String gatewayId = PLAYBACK_GATEWAY;
		PositionField pf = null;
		if(operation.isClose()) {
			for(PositionField pos : accStore.getPositions(gatewayId)) {
				boolean isOppositeDir = (operation.isBuy() && FieldUtils.isShort(pos.getPositionDirection()) 
						|| operation.isSell() && FieldUtils.isLong(pos.getPositionDirection()));
				if(ContractUtils.isSame(pos.getContract(), contract) && isOppositeDir) {
					pf = pos;
				}
			}
			if(pf == null) {
				mlog.warn("委托信息：{} {} {}手", contract.getUnifiedSymbol(), operation, volume);
				mlog.warn("持仓信息：{}", accStore);
				throw new IllegalStateException("没有找到对应的持仓进行操作");
			}
		}
		TickField lastTick = latestTickMap.get(contract.getUnifiedSymbol());
		TradeField trade = TradeField.newBuilder()
				.setGatewayId(PLAYBACK_GATEWAY)
				.setAccountId(PLAYBACK_GATEWAY)
				.setContract(contract)
				.setVolume(volume)
				.setPrice(lastTick.getLastPrice())
				.setTradeId(System.currentTimeMillis()+"")
				.setTradeTimestamp(lastTick.getActionTimestamp())
				.setDirection(OrderUtils.resolveDirection(operation))
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setOffsetFlag(closingStrategy.resolveOperation(operation, pf))
				.setPriceSource(PriceSourceEnum.PSRC_LastPrice)
				.setTradeDate(lastTick.getActionDay())
				.setTradingDay(lastTick.getTradingDay())
				.setTradeTime(LocalTime.parse(lastTick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER).format(DateTimeConstant.T_FORMAT_FORMATTER))
				.build();
		accStore.onTrade(trade);
		dealCollector.onTrade(trade).ifPresent(list -> list.stream().forEach(this.onDealCallback::accept));
		CompletableFuture.runAsync(() -> {
			tradeStrategy.onTrade(trade);
			onRuntimeChangeCallback.accept(getRuntimeDescription(false));
		}, CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));
		return Optional.of(id);
	}
	
	@Override
	public void submitOrderReq(TradeIntent tradeIntent) {
		// 回测时直接成交，没有撤单追单逻辑
		submitOrderReq(tradeIntent.getContract(), tradeIntent.getOperation(), tradeIntent.getPriceType(), tradeIntent.getVolume(), 0);
	}

	@Override
	public synchronized IDisposablePriceListener priceTriggerOut(String unifiedSymbol, DirectionEnum openDir,
			DisposablePriceListenerType listenerType, double basePrice, int numOfPriceTickToTrigger, int volume) {
		int factor = switch(listenerType) {
		case TAKE_PROFIT -> 1;
		case STOP_LOSS -> -1;
		default -> throw new IllegalArgumentException("Unexpected value: " + listenerType);
		};
		DisposablePriceListener listener = DisposablePriceListener.create(this, getContract(unifiedSymbol), openDir, basePrice, factor * Math.abs(numOfPriceTickToTrigger), volume);
		if(mlog.isInfoEnabled())
			mlog.info("增加【{}】", listener.description());
		listenerSet.add(listener);
		return listener;
	}

	@Override
	public synchronized IDisposablePriceListener priceTriggerOut(TradeField trade, DisposablePriceListenerType listenerType,
			int numOfPriceTickToTrigger) {
		return priceTriggerOut(trade.getContract().getUnifiedSymbol(), trade.getDirection(), listenerType, trade.getPrice(), numOfPriceTickToTrigger, trade.getVolume());
	}

	@Override
	public synchronized boolean isOrderWaitTimeout(String originOrderId, long timeout) {
		if(!orderReqMap.containsKey(originOrderId)) {
			return false;
		}
		
		SubmitOrderReqField orderReq = orderReqMap.get(originOrderId);
		TickField lastTick = latestTickMap.get(orderReq.getContract().getUnifiedSymbol());
		return lastTick.getActionTimestamp() - orderReq.getActionTimestamp() > timeout;
	}

	@Override
	public void cancelOrder(String originOrderId) {
		// 回测上下文不需要撤单
	}
	
	@Override
	public synchronized void onTick(TickField tick) {
		if(!bindedSymbolSet.contains(tick.getUnifiedSymbol())) {
			return;
		}
		mlog.trace("TICK信息: {} {} {}，最新价: {}", tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), tick.getLastPrice());
		indicatorFactory.getIndicatorMap().values().stream().forEach(indicator -> indicator.onTick(tick));
		comboIndicators.stream().forEach(combo -> combo.onTick(tick));
		accStore.onTick(tick);
		latestTickMap.put(tick.getUnifiedSymbol(), tick);
		listenerSet.stream()
			.filter(listener -> listener.shouldBeTriggered(tick))
			.forEach(listener -> {
				mlog.info("触发【{}】", listener.description());
				listener.execute();
			});
		tradeStrategy.onTick(tick);
		listenerSet = listenerSet.stream().filter(DisposablePriceListener::isValid).collect(Collectors.toSet());
	}

	@Override
	public synchronized void onBar(BarField bar) {
		if(!bindedSymbolSet.contains(bar.getUnifiedSymbol())) {
			return;
		}
		mlog.trace("分钟Bar信息: {} {} {}，最新价: {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getClosePrice());
		indicatorFactory.getIndicatorMap().entrySet().stream().forEach(e -> e.getValue().onBar(bar));	// 普通指标的更新
		comboIndicators.stream().forEach(combo -> combo.onBar(bar));
		inspectedValIndicatorFactory.getIndicatorMap().entrySet().stream().forEach(e -> e.getValue().onBar(bar));	// 值透视指标的更新
		registry.onBar(bar);
	}

	@Override
	public synchronized void onOrder(OrderField order) {
		// 回测上下文不接收外部的订单数据
	}

	@Override
	public synchronized void onTrade(TradeField trade) {
		// 回测上下文不接收外部的成交数据
	}

	@Override
	public int numOfMinPerModuleBar() {
		return numOfMinsPerBar;
	}

	@Override
	public synchronized int holdingNetProfit() {
		return accStore.getPositions(PLAYBACK_GATEWAY)
				.stream()
				.mapToInt(pf -> (int)pf.getPositionProfit())
				.sum();
	}

	@Override
	public synchronized int availablePosition(DirectionEnum direction, String unifiedSymbol) {
		return accStore.getPositions(PLAYBACK_GATEWAY)
				.stream()
				.filter(pf -> StringUtils.equals(pf.getContract().getUnifiedSymbol(), unifiedSymbol))
				.filter(pf -> FieldUtils.isLong(pf.getPositionDirection()) && FieldUtils.isBuy(direction) || FieldUtils.isShort(pf.getPositionDirection()) && FieldUtils.isSell(direction))
				.mapToInt(pf -> pf.getPosition() - pf.getFrozen())
				.sum();
	}

	@Override
	public synchronized int availablePosition(DirectionEnum direction, String unifiedSymbol, boolean isToday) {
		Stream<PositionField> posStream = accStore.getPositions(PLAYBACK_GATEWAY).stream()
				.filter(pf -> StringUtils.equals(pf.getContract().getUnifiedSymbol(), unifiedSymbol))
				.filter(pf -> FieldUtils.isLong(pf.getPositionDirection()) && FieldUtils.isBuy(direction) || FieldUtils.isShort(pf.getPositionDirection()) && FieldUtils.isSell(direction));
		
		if(isToday)	return posStream.mapToInt(pf -> pf.getTdPosition() - pf.getTdFrozen()).sum();
		return posStream.mapToInt(pf -> pf.getYdPosition() - pf.getYdFrozen()).sum(); 
	}

	@Override
	public ModuleState getState() {
		return accStore.getModuleState();
	}

	@Override
	public synchronized void disabledModule() {
		module.setEnabled(false);
	}

	@Override
	public Logger getLogger() {
		return mlog;
	}

	@Override
	public synchronized Indicator newIndicator(Configuration configuration, ValueType valueType, TimeSeriesUnaryOperator indicatorFunction) {
		Assert.isTrue(configuration.getNumOfUnits() > 0, "周期数必须大于0，当前为：" + configuration.getNumOfUnits());
		Assert.isTrue(configuration.getIndicatorRefLength() > 0, "指标回溯长度必须大于0，当前为：" + configuration.getIndicatorRefLength());
		Indicator in = indicatorFactory.newIndicator(configuration, valueType, indicatorFunction);
		indicatorValBufQMap.put(in, new LinkedList<>());
		registry.addListener(contractMap2.get(configuration.getBindedContract()), configuration.getNumOfUnits(), configuration.getPeriod(), in, ListenerType.INDICATOR);
		return in;
	}

	@Override
	public synchronized Indicator newIndicator(Configuration configuration, TimeSeriesUnaryOperator indicatorFunction) {
		return newIndicator(configuration, ValueType.CLOSE, indicatorFunction);
	}

	@Override
	public synchronized Indicator newIndicator(Configuration configuration, Function<BarWrapper, TimeSeriesValue> indicatorFunction) {
		Assert.isTrue(configuration.getNumOfUnits() > 0, "周期数必须大于0，当前为：" + configuration.getNumOfUnits());
		Assert.isTrue(configuration.getIndicatorRefLength() > 0, "指标回溯长度必须大于0，当前为：" + configuration.getIndicatorRefLength());
		Indicator in = indicatorFactory.newIndicator(configuration, indicatorFunction);
		indicatorValBufQMap.put(in, new LinkedList<>());
		registry.addListener(contractMap2.get(configuration.getBindedContract()), configuration.getNumOfUnits(), configuration.getPeriod(), in, ListenerType.INDICATOR);
		return in;
	}
	
	@Override
	public synchronized void viewValueAsIndicator(Configuration configuration, AtomicDouble value) {
		Indicator in = inspectedValIndicatorFactory.newIndicator(configuration, bar -> new TimeSeriesValue(value.get(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
		indicatorValBufQMap.put(in, new LinkedList<>());
		registry.addListener(contractMap2.get(configuration.getBindedContract()), configuration.getNumOfUnits(), configuration.getPeriod(), in, ListenerType.INSPECTABLE_VAL);
	}

	@Override
	public synchronized void addComboIndicator(IComboIndicator comboIndicator) {
		comboIndicators.add(comboIndicator);
		Contract c = contractMap2.get(comboIndicator.getConfiguration().getBindedContract());
		int numOfUnits = comboIndicator.getConfiguration().getNumOfUnits();
		PeriodUnit unit = comboIndicator.getConfiguration().getPeriod();
		registry.addListener(c, numOfUnits, unit, comboIndicator, ListenerType.COMBO_INDICATOR);
	}
	
	@Override
	public TradeStrategy getTradeStrategy() {
		return tradeStrategy;
	}
	
	@Override
	public TradeGateway getTradeGateway(ContractField contract) {
		return gatewayMap.get(contract);
	}

	@Override
	public synchronized ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
		Map<String, ModuleAccountRuntimeDescription> accMap = new HashMap<>();
		String gatewayId = PLAYBACK_GATEWAY;
		ModulePositionDescription posDescription = ModulePositionDescription.builder()
				.logicalPositions(accStore.getPositions(gatewayId).stream().map(PositionField::toByteArray).toList())
				.uncloseTrades(accStore.getUncloseTrades(gatewayId).stream().map(TradeField::toByteArray).toList())
				.build();
		
		ModuleAccountRuntimeDescription accDescription = ModuleAccountRuntimeDescription.builder()
				.accountId(gatewayId)
				.initBalance(accStore.getInitBalance(gatewayId))
				.preBalance(accStore.getPreBalance(gatewayId))
				.accCloseProfit(accStore.getAccCloseProfit(gatewayId))
				.accDealVolume(accStore.getAccDealVolume(gatewayId))
				.accCommission(accStore.getAccCommission(gatewayId))
				.maxDrawBack(accStore.getMaxDrawBack(gatewayId))
				.maxProfit(accStore.getMaxProfit(gatewayId))
				.positionDescription(posDescription)
				.build();
		accMap.put(gatewayId, accDescription);
		
		ModuleRuntimeDescription mad = ModuleRuntimeDescription.builder()
				.moduleName(module.getName())
				.enabled(module.isEnabled())
				.moduleState(accStore.getModuleState())
				.dataState(tradeStrategy.getComputedState())
				.accountRuntimeDescriptionMap(accMap)
				.build();
		if(fullDescription) {
			Map<String, List<String>> indicatorMap = new HashMap<>();
			Map<String, LinkedHashMap<Long, JSONObject>> symbolTimeObject = new HashMap<>();
			barBufQMap.entrySet().forEach(e -> 
				e.getValue().forEach(bar -> {
					if(!symbolTimeObject.containsKey(bar.getUnifiedSymbol())) {
						symbolTimeObject.put(bar.getUnifiedSymbol(), new LinkedHashMap<>());
					}
					symbolTimeObject.get(bar.getUnifiedSymbol()).put(bar.getActionTimestamp(), assignBar(bar));
				})
			);
			
			indicatorValBufQMap.entrySet().forEach(e -> {
				Indicator in = e.getKey();
				if(!indicatorMap.containsKey(in.bindedUnifiedSymbol())) {
					indicatorMap.put(in.bindedUnifiedSymbol(), new ArrayList<>());
				}
				indicatorMap.get(in.bindedUnifiedSymbol()).add(in.name());
				Collections.sort(indicatorMap.get(in.bindedUnifiedSymbol()));
				
				e.getValue().stream().forEach(tv -> {
					if(!symbolTimeObject.containsKey(in.bindedUnifiedSymbol())
							|| !symbolTimeObject.get(in.bindedUnifiedSymbol()).containsKey(tv.getTimestamp())) {
						return;
					}
					symbolTimeObject.get(in.bindedUnifiedSymbol()).get(tv.getTimestamp()).put(in.name(), tv.getValue());
				});
			});
			Map<String, JSONArray> dataMap = barBufQMap.entrySet().stream().collect(Collectors.toMap(
					Entry::getKey, 
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
	
	private JSONObject assignBar(BarField bar) {
		JSONObject json = new JSONObject();
		json.put("open", bar.getOpenPrice());
		json.put("low", bar.getLowPrice());
		json.put("high", bar.getHighPrice());
		json.put("close", bar.getClosePrice());
		json.put("volume", bar.getVolume());
		json.put("openInterestDelta", bar.getOpenInterestDelta());
		json.put("openInterest", bar.getOpenInterest());
		json.put("timestamp", bar.getActionTimestamp());
		return json;
	}

	@Override
	public synchronized void bindGatewayContracts(TradeGateway gateway, List<Contract> contracts) {
		for(Contract contract : contracts) {
			ContractField c = contract.contractField();
			gatewayMap.put(c, gateway);
			contractMap.put(c.getUnifiedSymbol(), c);
			contractMap2.put(c, contract);
			barBufQMap.put(c.getUnifiedSymbol(), new LinkedList<>());
			bindedSymbolSet.add(c.getUnifiedSymbol());
			registry.addListener(contract, numOfMinsPerBar, PeriodUnit.MINUTE, tradeStrategy, ListenerType.STRATEGY);
			registry.addListener(contract, numOfMinsPerBar, PeriodUnit.MINUTE, this, ListenerType.CONTEXT);
		}
	}

	@Override
	public synchronized void setModule(IModule module) {
		this.module = module;
		tradeStrategy.setContext(this);
	}

	@Override
	public void onMergedBar(BarField bar) {
		mlog.debug("合并Bar信息: {} {} {} {}，最新价: {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getActionTimestamp(), bar.getClosePrice());
		Consumer<Map.Entry<String,Indicator>> action = e -> {
			Indicator indicator = e.getValue();
			if(!StringUtils.equals(indicator.bindedUnifiedSymbol(), bar.getUnifiedSymbol())) {
				return;
			}
			if(indicatorValBufQMap.get(indicator).size() >= bufSize.intValue()) {
				indicatorValBufQMap.get(indicator).poll();
			}
			if(indicator.isReady() && indicator.timeSeriesValue(0).getTimestamp() == bar.getActionTimestamp()	// 只有时间戳一致才会被记录
					&& (indicator.value(0) != Double.MIN_VALUE && indicator.value(0) != Double.MAX_VALUE && !Double.isNaN(indicator.value(0)))	// 忽略潜在的初始值
					&& (BarUtils.isEndOfTheTradingDay(bar) || indicator.ifPlotPerBar() || !indicator.timeSeriesValue(0).isUnsettled())) {		
				indicatorValBufQMap.get(indicator).offer(indicator.timeSeriesValue(0));	
			}
		};
		try {			
			indicatorFactory.getIndicatorMap().entrySet().stream().forEach(action);	// 记录常规指标更新值 
			inspectedValIndicatorFactory.getIndicatorMap().entrySet().stream().forEach(action);	// 记录透视值更新
		} catch(Exception e) {
			getLogger().error("", e);
		}
		if(barBufQMap.get(bar.getUnifiedSymbol()).size() >= bufSize.intValue()) {
			barBufQMap.get(bar.getUnifiedSymbol()).poll();
		}
		barBufQMap.get(bar.getUnifiedSymbol()).offer(bar);		
	}

	@Override
	public void sendNotification(String content) {
		// 回测时不实现该方法
	}

}
