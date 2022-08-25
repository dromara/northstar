package tech.quantit.northstar.domain.module;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.common.model.IndicatorData;
import tech.quantit.northstar.common.model.ModuleAccountRuntimeDescription;
import tech.quantit.northstar.common.model.ModuleDealRecord;
import tech.quantit.northstar.common.model.ModulePositionDescription;
import tech.quantit.northstar.common.model.ModuleRuntimeDescription;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.common.utils.ContractUtils;
import tech.quantit.northstar.common.utils.FieldUtils;
import tech.quantit.northstar.common.utils.OrderUtils;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import tech.quantit.northstar.strategy.api.IDisposablePriceListener;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.IndicatorFactory;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import tech.quantit.northstar.strategy.api.log.ModuleLoggerFactory;
import tech.quantit.northstar.strategy.api.utils.trade.DisposablePriceListener;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.PositionField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 模组上下文
 * @author KevinHuangwl
 *
 */
@Slf4j
public class ModuleContext implements IModuleContext{
	
	private static final ILoggerFactory logFactory = new ModuleLoggerFactory();
	
	protected TradeStrategy tradeStrategy;
	
	protected IModuleAccountStore accStore;
	
	protected ClosingStrategy closingStrategy;
	
	protected IModule module;
	
	/* originOrderId -> orderReq */
	private Map<String, SubmitOrderReqField> orderReqMap = new HashMap<>();
	
	/* contract -> gateway */
	private Map<ContractField, TradeGateway> gatewayMap = new HashMap<>();
	
	/* unifiedSymbol -> contract */
	private Map<String, ContractField> contractMap = new HashMap<>();
	
	private Set<String> bindedSymbolSet = new HashSet<>();
	private Set<BarMerger> contractBarMergerSet = new HashSet<>();
	
	/* unifiedSymbol -> tick */
	private Map<String, TickField> latestTickMap = new HashMap<>();
	
	/* unifiedSymbol -> barQ */
	private Map<String, Queue<BarField>> barBufQMap = new HashMap<>();
	
	/* indicatorName -> values */
	private Map<String, Queue<TimeSeriesValue>> indicatorValBufQMap = new HashMap<>(); 
	
	private Set<DisposablePriceListener> listenerSet = new HashSet<>();
	
	private String tradingDay = "";
	
	private int numOfMinsPerBar;
	
	private DealCollector dealCollector;
	
	private Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback;
	
	private Consumer<ModuleDealRecord> onDealCallback;
	
	private final IndicatorFactory indicatorFactory = new IndicatorFactory();	// 基础周期指标工厂
	
	private final IndicatorFactory periodicIndicatorFactory = new IndicatorFactory();	// 额外周期指标工厂
	
	private final AtomicInteger bufSize = new AtomicInteger(0);
	
	private Consumer<BarField> barMergingCallback = bar -> {
		indicatorFactory.getIndicatorMap().entrySet().stream().forEach(e -> {
			Indicator indicator = e.getValue();
			indicator.onBar(bar);
			if(indicatorValBufQMap.get(e.getKey()).size() >= bufSize.intValue()) {
				indicatorValBufQMap.get(e.getKey()).poll();
			}
			if(indicator.isReady() && indicator.valueWithTime(0).getTimestamp() == bar.getActionTimestamp()) {		// 只有时间戳一致才会被记录			
				indicatorValBufQMap.get(e.getKey()).offer(indicator.valueWithTime(0));	
			}
		});
		tradeStrategy.onBar(bar, module.isEnabled());
		if(barBufQMap.get(bar.getUnifiedSymbol()).size() >= bufSize.intValue()) {
			barBufQMap.get(bar.getUnifiedSymbol()).poll();
		}
		barBufQMap.get(bar.getUnifiedSymbol()).offer(bar);
	};
	
	private final String moduleName;
	
	private Logger mlog;
	
	public ModuleContext(String name, TradeStrategy tradeStrategy, IModuleAccountStore accStore, ClosingStrategy closingStrategy, int numOfMinsPerBar, 
			int bufSize, DealCollector dealCollector, Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback, Consumer<ModuleDealRecord> onDealCallback) {
		this.moduleName = name;
		this.mlog = logFactory.getLogger(name);
		this.tradeStrategy = tradeStrategy;
		this.accStore = accStore;
		this.closingStrategy = closingStrategy;
		this.numOfMinsPerBar = numOfMinsPerBar;
		this.dealCollector = dealCollector;
		this.onRuntimeChangeCallback = onRuntimeChangeCallback;
		this.onDealCallback = onDealCallback;
		this.bufSize.set(bufSize);
	}

	@Override
	public synchronized ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
		Map<String, ModuleAccountRuntimeDescription> accMap = new HashMap<>();
		for(TradeGateway gateway : gatewayMap.values()) {
			String gatewayId = gateway.getGatewaySetting().getGatewayId();
			if(accMap.containsKey(gatewayId)) {
				continue;
			}
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
		}
		
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
			indicatorFactory.getIndicatorMap().entrySet().stream()
					.filter(e -> indicatorValBufQMap.containsKey(e.getKey()))
					.forEach(e -> 
						indicatorMap.put(e.getKey(), IndicatorData.builder()
								.unifiedSymbol(e.getValue().bindedUnifiedSymbol())
								.type(e.getValue().getType())
								.values(indicatorValBufQMap.get(e.getKey()).stream().toList())
								.build())
					);
			periodicIndicatorFactory.getIndicatorMap().entrySet().stream()
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

	@Override
	public synchronized String submitOrderReq(ContractField contract, SignalOperation operation,
			PriceType priceType, int volume, double price) {
		if(mlog.isInfoEnabled()) {			
			mlog.info("策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", contract.getUnifiedSymbol(), operation.text(), price, volume, priceType);
		}
		if(!gatewayMap.containsKey(contract)) {
			throw new NoSuchElementException(String.format("找不到合约 [%s] 对应网关", contract.getUnifiedSymbol()));
		}
		String id = UUID.randomUUID().toString();
		String gatewayId = gatewayMap.get(contract).getGatewaySetting().getGatewayId();
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
		return submitOrderReq(SubmitOrderReqField.newBuilder()
				.setOriginOrderId(id)
				.setContract(contract)
				.setGatewayId(gatewayId)
				.setDirection(OrderUtils.resolveDirection(operation))
				.setOffsetFlag(closingStrategy.resolveOperation(operation, pf))
				.setPrice(price)
				.setVolume(volume)		//	当信号交易量大于零时，优先使用信号交易量
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.setOrderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setActionTimestamp(latestTickMap.get(contract.getUnifiedSymbol()).getActionTimestamp())
				.setMinVolume(1)
				.build());
	}

	DateFormat fmt = new SimpleDateFormat();
	private String submitOrderReq(SubmitOrderReqField orderReq) {
		if(mlog.isInfoEnabled()) {			
			mlog.info("发单：{}，{}", orderReq.getOriginOrderId(), fmt.format(new Date(orderReq.getActionTimestamp())));
		}
		if(FieldUtils.isOpen(orderReq.getOffsetFlag())) {
			checkAmount(orderReq);
		}
		ContractField contract = orderReq.getContract();
		TradeGateway gateway = gatewayMap.get(contract);
		gateway.submitOrder(orderReq);
		orderReqMap.put(orderReq.getOriginOrderId(), orderReq);
		accStore.onSubmitOrder(orderReq);
		return orderReq.getOriginOrderId();
	}
	
	private void checkAmount(SubmitOrderReqField orderReq) {
		double orderPrice = orderReq.getOrderPriceType() == OrderPriceTypeEnum.OPT_AnyPrice ? latestTickMap.get(orderReq.getContract().getUnifiedSymbol()).getLastPrice() : orderReq.getPrice();
		double extMargin = orderReq.getVolume() * orderPrice * orderReq.getContract().getMultiplier() * FieldUtils.marginRatio(orderReq.getContract(), orderReq.getDirection());
		double preBalance = accStore.getPreBalance(orderReq.getGatewayId());
		if(preBalance < extMargin) {
			throw new TradeException(String.format("模组可用资金 [%s] 小于开仓保证金 [%s]", preBalance, extMargin));
		}
	}
	
	@Override
	public IDisposablePriceListener priceTriggerOut(String unifiedSymbol, DirectionEnum openDir, double basePrice, int numOfPriceTickToTrigger, int volume) {
		return priceTriggerOut(getContract(unifiedSymbol), openDir, basePrice, numOfPriceTickToTrigger, volume);
	}
	
	@Override
	public IDisposablePriceListener priceTriggerOut(ContractField contract, DirectionEnum openDir, double basePrice, int numOfPriceTickToTrigger, int volume) {
		DisposablePriceListener listener = DisposablePriceListener.create(this, contract, openDir, basePrice, numOfPriceTickToTrigger, volume);
		if(mlog.isInfoEnabled())
			mlog.info("增加【{}】", listener.description());
		listenerSet.add(listener);
		return listener;
	}

	@Override
	public synchronized void cancelOrder(String originOrderId) {
		if(!orderReqMap.containsKey(originOrderId)) {
			mlog.debug("找不到订单：{}", originOrderId);
			return;
		}
		mlog.info("撤单：{}", originOrderId);
		ContractField contract = orderReqMap.get(originOrderId).getContract();
		TradeGateway gateway = gatewayMap.get(contract);
		CancelOrderReqField cancelReq = CancelOrderReqField.newBuilder()
				.setGatewayId(gateway.getGatewaySetting().getGatewayId())
				.setOriginOrderId(originOrderId)
				.build();
		accStore.onCancelOrder(cancelReq);
		gateway.cancelOrder(cancelReq);
	}
	
	@Override
	public int holdingNetProfit() {
		return gatewayMap.values().stream()
				.map(gw -> gw.getGatewaySetting().getGatewayId())
				.map(gatewayId -> accStore.getPositions(gatewayId))
				.flatMap(Collection::stream)
				.mapToInt(pf -> (int)pf.getPositionProfit())
				.sum();
	}
	
	@Override
	public int availablePosition(DirectionEnum direction, String unifiedSymbol) {
		return gatewayMap.values().stream()
				.map(gw -> gw.getGatewaySetting().getGatewayId())
				.map(gatewayId -> accStore.getPositions(gatewayId))
				.flatMap(Collection::stream)
				.filter(pf -> StringUtils.equals(pf.getContract().getUnifiedSymbol(), unifiedSymbol))
				.mapToInt(pf -> pf.getPosition() - pf.getFrozen())
				.sum();
	}
	
	@Override
	public int availablePosition(DirectionEnum direction, String unifiedSymbol, boolean isToday) {
		Stream<PositionField> posStream = gatewayMap.values().stream()
				.map(gw -> gw.getGatewaySetting().getGatewayId())
				.map(gatewayId -> accStore.getPositions(gatewayId))
				.flatMap(Collection::stream)
				.filter(pf -> StringUtils.equals(pf.getContract().getUnifiedSymbol(), unifiedSymbol));
		
		if(isToday)	return posStream.mapToInt(pf -> pf.getTdPosition() - pf.getTdFrozen()).sum();
		return posStream.mapToInt(pf -> pf.getYdPosition() - pf.getYdFrozen()).sum(); 
	}

	/* 此处收到的TICK数据是所有订阅的数据，需要过滤 */
	@Override
	public synchronized void onTick(TickField tick) {
		if(!bindedSymbolSet.contains(tick.getUnifiedSymbol())) {
			return;
		}
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
		}
		accStore.onTick(tick);
		latestTickMap.put(tick.getUnifiedSymbol(), tick);
		listenerSet.stream()
			.filter(listener -> listener.shouldBeTriggered(tick))
			.forEach(listener -> {
				mlog.info("触发【{}】", listener.description());
				log.info("模组[{}] 触发【{}】", moduleName, listener.description());
				listener.execute();
			});
		tradeStrategy.onTick(tick, module.isEnabled());
		listenerSet = listenerSet.stream().filter(DisposablePriceListener::isValid).collect(Collectors.toSet());
	}
	
	/* 此处收到的BAR数据是所有订阅的数据，需要过滤 */
	@Override
	public synchronized void onBar(BarField bar) {
		if(!bindedSymbolSet.contains(bar.getUnifiedSymbol())) {
			return;
		}
		contractBarMergerSet.forEach(barMerger -> barMerger.updateBar(bar));
	}
	
	/* 此处收到的ORDER数据是所有订单回报，需要过滤 */
	@Override
	public synchronized void onOrder(OrderField order) {
		if(!orderReqMap.containsKey(order.getOriginOrderId())) {
			return;
		}
		if(!OrderUtils.isValidOrder(order)) {
			orderReqMap.remove(order.getOriginOrderId());
		}
		accStore.onOrder(order);
		tradeStrategy.onOrder(order);
	}

	/* 此处收到的TRADE数据是所有成交回报，需要过滤 */
	@Override
	public synchronized void onTrade(TradeField trade) {
		if(!orderReqMap.containsKey(trade.getOriginOrderId()) && !StringUtils.equals(trade.getOriginOrderId(), Constants.MOCK_ORDER_ID)) {
			return;
		}
		if(orderReqMap.containsKey(trade.getOriginOrderId())) {
			mlog.info("成交：{}， 操作：{}{}， 价格：{}， 手数：{}", trade.getOriginOrderId(), FieldUtils.chn(trade.getDirection()), 
					FieldUtils.chn(trade.getOffsetFlag()), trade.getPrice(), trade.getVolume());
			orderReqMap.remove(trade.getOriginOrderId());
		}
		accStore.onTrade(trade);
		tradeStrategy.onTrade(trade);
		onRuntimeChangeCallback.accept(getRuntimeDescription(false));
		dealCollector.onTrade(trade).ifPresent(list -> list.stream().forEach(this.onDealCallback::accept));
		
		if(getState().isEmpty() && !listenerSet.isEmpty()) {
			mlog.info("净持仓为零，止盈止损监听器被清除");
			listenerSet.clear();
		}
	}

	@Override
	public TradeStrategy getTradeStrategy() {
		return tradeStrategy;
	}

	@Override
	public void disabledModule() {
		module.setEnabled(false);
	}

	@Override
	public synchronized void setModule(IModule module) {
		this.module = module;
		tradeStrategy.setContext(this);
	}

	@Override
	public String getModuleName() {
		return moduleName;
	}

	@Override
	public synchronized void bindGatewayContracts(TradeGateway gateway, List<ContractField> contracts) {
		for(ContractField c : contracts) {			
			gatewayMap.put(c, gateway);
			contractMap.put(c.getUnifiedSymbol(), c);
			barBufQMap.put(c.getUnifiedSymbol(), new LinkedList<>());
			contractBarMergerSet.add(new BarMerger(numOfMinsPerBar, c, barMergingCallback));
			bindedSymbolSet.add(c.getUnifiedSymbol());
		}
	}

	@Override
	public ContractField getContract(String unifiedSymbol) {
		if(!contractMap.containsKey(unifiedSymbol)) {
			throw new NoSuchElementException("找不到合约：" + unifiedSymbol);
		}
		return contractMap.get(unifiedSymbol);
	}

	@Override
	public ModuleState getState() {
		return accStore.getModuleState();
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
	public Logger getLogger() {
		return mlog;
	}

	@Override
	public Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength,
			ValueType valTypeOfBar, TimeSeriesUnaryOperator valueUpdateHandler) {
		indicatorValBufQMap.put(indicatorName, new LinkedList<>());
		return indicatorFactory.newIndicator(indicatorName, bindedUnifiedSymbol, indicatorLength, valTypeOfBar, valueUpdateHandler);
	}

	@Override
	public Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol,
			TimeSeriesUnaryOperator valueUpdateHandler) {
		return newIndicator(indicatorName, bindedUnifiedSymbol, 16, ValueType.CLOSE, valueUpdateHandler);
	}
	
	@Override
	public Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength,
			TimeSeriesUnaryOperator valueUpdateHandler) {
		return newIndicator(indicatorName, bindedUnifiedSymbol, indicatorLength, ValueType.CLOSE, valueUpdateHandler);
	}

	@Override
	public Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength,
			Function<BarField, TimeSeriesValue> valueUpdateHandler) {
		indicatorValBufQMap.put(indicatorName, new LinkedList<>());
		return indicatorFactory.newIndicator(indicatorName, bindedUnifiedSymbol, indicatorLength, valueUpdateHandler);
	}

	@Override
	public Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol,
			Function<BarField, TimeSeriesValue> valueUpdateHandler) {
		return newIndicator(indicatorName, bindedUnifiedSymbol, 16, valueUpdateHandler);
	}
	
	@Override
	public Indicator newIndicatorAtPeriod(int numOfMinPerPeriod, String indicatorName, String bindedUnifiedSymbol,
			int indicatorLength, Function<BarField, TimeSeriesValue> indicatorFunction) {
		if(numOfMinPerPeriod < 1) {
			throw new IllegalStateException("非法指标周期，期望周期数大于0。实际: " + numOfMinPerPeriod);
		}
		final String indicatorNameWithPeriod = String.format("%s_%dM", indicatorName, numOfMinPerPeriod);
		indicatorValBufQMap.put(indicatorNameWithPeriod, new LinkedList<>());
		contractBarMergerSet.add(new BarMerger(numOfMinPerPeriod, contractMap.get(bindedUnifiedSymbol), bar -> {
			Indicator indicator = periodicIndicatorFactory.getIndicatorMap().get(indicatorNameWithPeriod);
			indicator.onBar(bar);
			if(indicatorValBufQMap.get(indicatorNameWithPeriod).size() >= bufSize.intValue()) {
				indicatorValBufQMap.get(indicatorNameWithPeriod).poll();
			}
			if(indicator.isReady()) {					
				indicatorValBufQMap.get(indicatorNameWithPeriod).offer(indicator.valueWithTime(0));
			}
		}));
		return periodicIndicatorFactory.newIndicator(indicatorNameWithPeriod, bindedUnifiedSymbol, indicatorLength, indicatorFunction);
	}

	@Override
	public Indicator newIndicatorAtPeriod(int numOfMinPerPeriod, String indicatorName, String bindedUnifiedSymbol,
			Function<BarField, TimeSeriesValue> indicatorFunction) {
		return newIndicatorAtPeriod(numOfMinPerPeriod, indicatorName, bindedUnifiedSymbol, 16, indicatorFunction);
	}

	@Override
	public Indicator newIndicatorAtPeriod(int numOfMinPerPeriod, String indicatorName, String bindedUnifiedSymbol,
			int indicatorLength, ValueType valueTypeOfBar, TimeSeriesUnaryOperator indicatorFunction) {
		if(numOfMinPerPeriod < 1) {
			throw new IllegalStateException("非法指标周期，期望周期数大于0。实际: " + numOfMinPerPeriod);
		}
		final String indicatorNameWithPeriod = String.format("%s_%dM", indicatorName, numOfMinPerPeriod);
		indicatorValBufQMap.put(indicatorNameWithPeriod, new LinkedList<>());
		contractBarMergerSet.add(new BarMerger(numOfMinPerPeriod, contractMap.get(bindedUnifiedSymbol), bar -> {
			Indicator indicator = periodicIndicatorFactory.getIndicatorMap().get(indicatorNameWithPeriod);
			indicator.onBar(bar);
			if(indicatorValBufQMap.get(indicatorNameWithPeriod).size() >= bufSize.intValue()) {
				indicatorValBufQMap.get(indicatorNameWithPeriod).poll();
			}
			if(indicator.isReady()) {					
				indicatorValBufQMap.get(indicatorNameWithPeriod).offer(indicator.valueWithTime(0));
			}
		}));
		return periodicIndicatorFactory.newIndicator(indicatorNameWithPeriod, bindedUnifiedSymbol, indicatorLength, valueTypeOfBar, indicatorFunction);
	}

	@Override
	public Indicator newIndicatorAtPeriod(int numOfBarPerPeriod, String indicatorName, String bindedUnifiedSymbol,
			TimeSeriesUnaryOperator indicatorFunction) {
		return newIndicatorAtPeriod(numOfBarPerPeriod, indicatorName, bindedUnifiedSymbol, 16, ValueType.CLOSE, indicatorFunction);
	}

	@Override
	public Indicator newIndicatorAtPeriod(int numOfMinPerPeriod, String indicatorName, String bindedUnifiedSymbol,
			int indicatorLength, TimeSeriesUnaryOperator indicatorFunction) {
		return newIndicatorAtPeriod(numOfMinPerPeriod, indicatorName, bindedUnifiedSymbol, indicatorLength, ValueType.CLOSE, indicatorFunction);
	}

	@Override
	public boolean explain(boolean expression, String infoMessage, Object... args) {
		if(expression) {
			mlog.info(infoMessage, args);
		}
		return expression;
	}

}
