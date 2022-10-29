package tech.quantit.northstar.domain.module;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import com.google.common.util.concurrent.AtomicDouble;

import cn.hutool.core.lang.Assert;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.BarWrapper;
import tech.quantit.northstar.common.model.IndicatorData;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModulePositionDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.common.utils.BarUtils;
import tech.quantit.northstar.common.utils.ContractUtils;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.common.utils.OrderUtils;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import tech.quantit.northstar.strategy.api.IComboIndicator;
import tech.quantit.northstar.strategy.api.IDisposablePriceListener;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.IndicatorFactory;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.constant.DisposablePriceListenerType;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.Configuration;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import tech.quantit.northstar.strategy.api.log.ModuleLoggerFactory;
import tech.quantit.northstar.strategy.api.utils.bar.BarMerger;
import tech.quantit.northstar.strategy.api.utils.trade.DisposablePriceListener;
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
public class ModulePlaybackContext implements IModuleContext {
	
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
	/* unifiedSymbol -> barMerger */
	private Map<String, BarMerger> contractBarMergerMap = new HashMap<>();
	/* unifiedSymbol -> tick */
	private Map<String, TickField> latestTickMap = new HashMap<>();
	
	/* unifiedSymbol -> barQ */
	private Map<String, Queue<BarField>> barBufQMap = new HashMap<>();
	
	/* indicatorName -> values */
	private Map<String, Queue<TimeSeriesValue>> indicatorValBufQMap = new HashMap<>(); 
	
	private final AtomicInteger bufSize = new AtomicInteger(0);
	
	/* unifiedSymbol -> contract */
	private Map<String, ContractField> contractMap = new HashMap<>();
	
	private Set<DisposablePriceListener> listenerSet = new HashSet<>();
	
	private Logger mlog;
	
	private final IndicatorFactory indicatorFactory = new IndicatorFactory();
	
	private final IndicatorFactory inspectedValIndicatorFactory = new IndicatorFactory();
	
	private final HashSet<IComboIndicator> comboIndicators = new HashSet<>();
	
	private Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback;
	
	private Consumer<ModuleDealRecord> onDealCallback;
	
	private Consumer<BarField> barMergingCallback = bar -> {
		Consumer<Map.Entry<String,Indicator>> action = e -> {
			Indicator indicator = e.getValue();
			if(indicatorValBufQMap.get(e.getKey()).size() >= bufSize.intValue()) {
				indicatorValBufQMap.get(e.getKey()).poll();
			}
			if(indicator.isReady() && indicator.timeSeriesValue(0).getTimestamp() == bar.getActionTimestamp()	// 只有时间戳一致才会被记录
					&& (BarUtils.isEndOfTheTradingDay(bar) || indicator.ifPlotPerBar() || !indicator.timeSeriesValue(0).isUnsettled())) {		
				indicatorValBufQMap.get(e.getKey()).offer(indicator.timeSeriesValue(0));	
			}
		};
		indicatorFactory.getIndicatorMap().entrySet().stream().forEach(action);	// 记录常规指标更新值 
		tradeStrategy.onBar(bar, module.isEnabled());
		inspectedValIndicatorFactory.getIndicatorMap().entrySet().stream().forEach(action);	// 记录透视值更新
		if(barBufQMap.get(bar.getUnifiedSymbol()).size() >= bufSize.intValue()) {
			barBufQMap.get(bar.getUnifiedSymbol()).poll();
		}
		barBufQMap.get(bar.getUnifiedSymbol()).offer(bar);
	};
	
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
	public Optional<String> submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume,
			double price) {
		if(!module.isEnabled()) {
			mlog.info("策略处于停用状态，忽略委托单");
			return Optional.empty();
		}
		if(mlog.isInfoEnabled()) {			
			TickField tick = latestTickMap.get(contract.getUnifiedSymbol());
			mlog.info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
					tick.getActionDay(), LocalTime.parse(tick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER),
					contract.getUnifiedSymbol(), operation.text(), price, volume, priceType);
		}
		String id = UUID.randomUUID().toString();
		String gatewayId = PLAYBACK_GATEWAY;
		PositionField pf = null;
		for(PositionField pos : accStore.getPositions(gatewayId)) {
			boolean isOppositeDir = (operation.isBuy() && FieldUtils.isShort(pos.getPositionDirection()) 
					|| operation.isSell() && FieldUtils.isLong(pos.getPositionDirection()));
			if(ContractUtils.isSame(pos.getContract(), contract) && isOppositeDir) {
				pf = pos;
			}
		}
		if(pf == null && operation.isClose()) {
			throw new IllegalStateException("没有找到对应的持仓进行操作");
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
		onRuntimeChangeCallback.accept(getRuntimeDescription(false));
		dealCollector.onTrade(trade).ifPresent(list -> list.stream().forEach(this.onDealCallback::accept));
		return Optional.of(id);
	}
	
	

	@Override
	public IDisposablePriceListener priceTriggerOut(String unifiedSymbol, DirectionEnum openDir,
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
	public IDisposablePriceListener priceTriggerOut(TradeField trade, DisposablePriceListenerType listenerType,
			int numOfPriceTickToTrigger) {
		return priceTriggerOut(trade.getContract().getUnifiedSymbol(), trade.getDirection(), listenerType, trade.getPrice(), numOfPriceTickToTrigger, trade.getVolume());
	}

	@Override
	public boolean isOrderWaitTimeout(String originOrderId, long timeout) {
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
	public void onTick(TickField tick) {
		if(!bindedSymbolSet.contains(tick.getUnifiedSymbol())) {
			return;
		}
		mlog.trace("TICK信息: {} {} {}，最新价: {}", tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), tick.getLastPrice());
		indicatorFactory.getIndicatorMap().values().parallelStream().forEach(indicator -> indicator.onTick(tick));
		comboIndicators.parallelStream().forEach(combo -> combo.onTick(tick));
		accStore.onTick(tick);
		latestTickMap.put(tick.getUnifiedSymbol(), tick);
		listenerSet.stream()
			.filter(listener -> listener.shouldBeTriggered(tick))
			.forEach(listener -> {
				mlog.info("触发【{}】", listener.description());
				listener.execute();
			});
		tradeStrategy.onTick(tick, module.isEnabled());
		listenerSet = listenerSet.stream().filter(DisposablePriceListener::isValid).collect(Collectors.toSet());
	}

	@Override
	public void onBar(BarField bar) {
		if(!bindedSymbolSet.contains(bar.getUnifiedSymbol())) {
			return;
		}
		mlog.trace("Bar信息: {} {} {}，最新价: {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getClosePrice());
		indicatorFactory.getIndicatorMap().entrySet().parallelStream().forEach(e -> e.getValue().onBar(bar));	// 普通指标的更新
		comboIndicators.parallelStream().forEach(combo -> combo.onBar(bar));
		inspectedValIndicatorFactory.getIndicatorMap().entrySet().parallelStream().forEach(e -> e.getValue().onBar(bar));	// 值透视指标的更新
		contractBarMergerMap.get(bar.getUnifiedSymbol()).updateBar(bar);
	}

	@Override
	public void onOrder(OrderField order) {
		// 回测上下文不接收外部的订单数据
	}

	@Override
	public void onTrade(TradeField trade) {
		// 回测上下文不接收外部的成交数据
	}

	@Override
	public int numOfMinPerModuleBar() {
		return numOfMinsPerBar;
	}

	@Override
	public int holdingNetProfit() {
		return accStore.getPositions(PLAYBACK_GATEWAY)
				.stream()
				.mapToInt(pf -> (int)pf.getPositionProfit())
				.sum();
	}

	@Override
	public int availablePosition(DirectionEnum direction, String unifiedSymbol) {
		return accStore.getPositions(PLAYBACK_GATEWAY)
				.stream()
				.filter(pf -> StringUtils.equals(pf.getContract().getUnifiedSymbol(), unifiedSymbol))
				.filter(pf -> FieldUtils.isLong(pf.getPositionDirection()) && FieldUtils.isBuy(direction) || FieldUtils.isShort(pf.getPositionDirection()) && FieldUtils.isSell(direction))
				.mapToInt(pf -> pf.getPosition() - pf.getFrozen())
				.sum();
	}

	@Override
	public int availablePosition(DirectionEnum direction, String unifiedSymbol, boolean isToday) {
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
	public void disabledModule() {
		module.setEnabled(false);
	}

	@Override
	public Logger getLogger() {
		return mlog;
	}

	@Override
	public Indicator newIndicator(Configuration configuration, ValueType valueType, TimeSeriesUnaryOperator indicatorFunction) {
		Assert.isTrue(configuration.getNumOfUnits() > 0, "周期数必须大于0，当前为：" + configuration.getNumOfUnits());
		Assert.isTrue(configuration.getIndicatorRefLength() > 0, "指标回溯长度必须大于0，当前为：" + configuration.getIndicatorRefLength());
		indicatorValBufQMap.put(configuration.getIndicatorName(), new LinkedList<>());
		return indicatorFactory.newIndicator(configuration, valueType, indicatorFunction);
	}

	@Override
	public Indicator newIndicator(Configuration configuration, TimeSeriesUnaryOperator indicatorFunction) {
		return newIndicator(configuration, ValueType.CLOSE, indicatorFunction);
	}

	@Override
	public Indicator newIndicator(Configuration configuration, Function<BarWrapper, TimeSeriesValue> indicatorFunction) {
		Assert.isTrue(configuration.getNumOfUnits() > 0, "周期数必须大于0，当前为：" + configuration.getNumOfUnits());
		Assert.isTrue(configuration.getIndicatorRefLength() > 0, "指标回溯长度必须大于0，当前为：" + configuration.getIndicatorRefLength());
		indicatorValBufQMap.put(configuration.getIndicatorName(), new LinkedList<>());
		return indicatorFactory.newIndicator(configuration, indicatorFunction);
	}
	
	@Override
	public void viewValueAsIndicator(Configuration configuration, AtomicDouble value) {
		indicatorValBufQMap.put(configuration.getIndicatorName(), new LinkedList<>());
		inspectedValIndicatorFactory.newIndicator(configuration, bar -> new TimeSeriesValue(value.get(), bar.getBar().getActionTimestamp(), bar.isUnsettled()));
	}

	@Override
	public void addComboIndicator(IComboIndicator comboIndicator) {
		comboIndicators.add(comboIndicator);
	}
	
	@Override
	public TradeStrategy getTradeStrategy() {
		return tradeStrategy;
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
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
			mad.setBarDataMap(barBufQMap.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, 
							e -> e.getValue().stream().map(BarField::toByteArray).toList())));
			Map<String, IndicatorData> indicatorMap = new HashMap<>();
			indicatorFactorys().stream()
				.flatMap(factory -> factory.getIndicatorMap().entrySet().stream())
				.filter(e -> indicatorValBufQMap.containsKey(e.getKey()))
				.forEach(e -> 
					indicatorMap.put(e.getKey(), IndicatorData.builder()
							.unifiedSymbol(e.getValue().bindedUnifiedSymbol())
							.type(e.getValue().getType())
							.values(indicatorValBufQMap.get(e.getKey()).stream().toList())
							.build())
				);
			mad.setIndicatorMap(indicatorMap);
		}
		return mad;
	}
	
	private List<IndicatorFactory> indicatorFactorys(){
		return List.of(indicatorFactory, inspectedValIndicatorFactory);
	}

	@Override
	public void bindGatewayContracts(TradeGateway gateway, List<ContractField> contracts) {
		for(ContractField c : contracts) {			
			contractMap.put(c.getUnifiedSymbol(), c);
			barBufQMap.put(c.getUnifiedSymbol(), new LinkedList<>());
			bindedSymbolSet.add(c.getUnifiedSymbol());
			contractBarMergerMap.put(c.getUnifiedSymbol(), new BarMerger(numOfMinsPerBar, c, barMergingCallback));
		}
	}

	@Override
	public void setModule(IModule module) {
		this.module = module;
		tradeStrategy.setContext(this);
	}
	
}
