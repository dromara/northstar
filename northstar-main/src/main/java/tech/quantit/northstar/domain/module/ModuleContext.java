package tech.quantit.northstar.domain.module;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.util.concurrent.AtomicDouble;

import cn.hutool.core.lang.Assert;
import tech.quantit.northstar.common.constant.Constants;
import tech.quantit.northstar.common.constant.DateTimeConstant;
import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.exception.TradeException;
import tech.quantit.northstar.common.model.BarWrapper;
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
import tech.quantit.northstar.gateway.api.domain.contract.Contract;
import tech.quantit.northstar.main.mail.MailDeliveryManager;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import tech.quantit.northstar.strategy.api.IComboIndicator;
import tech.quantit.northstar.strategy.api.IDisposablePriceListener;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.IndicatorFactory;
import tech.quantit.northstar.strategy.api.MergedBarListener;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.constant.DisposablePriceListenerType;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.Configuration;
import tech.quantit.northstar.strategy.api.indicator.Indicator.PeriodUnit;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import tech.quantit.northstar.strategy.api.indicator.TimeSeriesUnaryOperator;
import tech.quantit.northstar.strategy.api.log.ModuleLoggerFactory;
import tech.quantit.northstar.strategy.api.utils.bar.BarMergerRegistry;
import tech.quantit.northstar.strategy.api.utils.bar.BarMergerRegistry.ListenerType;
import tech.quantit.northstar.strategy.api.utils.trade.DealCollector;
import tech.quantit.northstar.strategy.api.utils.trade.DisposablePriceListener;
import tech.quantit.northstar.strategy.api.utils.trade.TradeIntent;
import xyz.redtorch.pb.CoreEnum.CommonStatusEnum;
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
import xyz.redtorch.pb.CoreField.NoticeField;
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
public class ModuleContext implements IModuleContext, MergedBarListener{
	
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
	private Map<ContractField, Contract> contractMap2 = new HashMap<>();
	
	private Set<String> bindedSymbolSet = new HashSet<>();
	/* unifiedSymbol -> tick */
	private Map<String, TickField> latestTickMap = new HashMap<>();
	
	/* unifiedSymbol -> barQ */
	private Map<String, Queue<BarField>> barBufQMap = new HashMap<>();
	
	/* indicator -> values */
	private Map<Indicator, Queue<TimeSeriesValue>> indicatorValBufQMap = new HashMap<>(); 
	
	private Set<DisposablePriceListener> listenerSet = new HashSet<>();
	
	private String tradingDay = "";
	
	private int numOfMinsPerBar;
	
	private TradeIntent tradeIntent;	// 交易意图
	
	private DealCollector dealCollector;
	
	private Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback;
	
	private Consumer<ModuleDealRecord> onDealCallback;
	
	private final IndicatorFactory indicatorFactory = new IndicatorFactory(this);
	
	private final IndicatorFactory inspectedValIndicatorFactory = new IndicatorFactory(this);
	
	private final HashSet<IComboIndicator> comboIndicators = new HashSet<>();
	
	private final AtomicInteger bufSize = new AtomicInteger(0);
	
	private final String moduleName;
	
	private final BarMergerRegistry registry = new BarMergerRegistry();
	
	private Logger mlog;
	
	private MailDeliveryManager mailMgr;
	
	public ModuleContext(String name, TradeStrategy tradeStrategy, IModuleAccountStore accStore, ClosingStrategy closingStrategy, int numOfMinsPerBar, 
			int bufSize, DealCollector dealCollector, Consumer<ModuleRuntimeDescription> onRuntimeChangeCallback, Consumer<ModuleDealRecord> onDealCallback,
			MailDeliveryManager mailMgr) {
		this.moduleName = name;
		this.mlog = logFactory.getLogger(name);
		this.tradeStrategy = tradeStrategy;
		this.accStore = accStore;
		this.closingStrategy = closingStrategy;
		this.numOfMinsPerBar = numOfMinsPerBar;
		this.dealCollector = dealCollector;
		this.onRuntimeChangeCallback = onRuntimeChangeCallback;
		this.onDealCallback = onDealCallback;
		this.mailMgr = mailMgr;
		this.bufSize.set(bufSize);
	}

	@Override
	public synchronized ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
		Map<String, ModuleAccountRuntimeDescription> accMap = new HashMap<>();
		for(TradeGateway gateway : gatewayMap.values()) {
			String gatewayId = gateway.gatewayId();
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
					.maxDrawBack(accStore.getMaxDrawBack(gatewayId))
					.maxProfit(accStore.getMaxProfit(gatewayId))
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
	public synchronized Optional<String> submitOrderReq(ContractField contract, SignalOperation operation,
			PriceType priceType, int volume, double price) {
		if(!module.isEnabled()) {
			mlog.info("策略处于停用状态，忽略委托单");
			return Optional.empty();
		}
		TickField tick = latestTickMap.get(contract.getUnifiedSymbol());
		Assert.notNull(tick, "没有行情时不应该发送订单");
		Assert.isTrue(volume > 0, "下单手数应该为正数");
		
		double orderPrice = switch(priceType) {
		case ANY_PRICE -> 0;
		case LAST_PRICE -> tick.getLastPrice();
		case LIMIT_PRICE -> price;
		case OPP_PRICE -> operation.isBuy() ? tick.getAskPrice(0) : tick.getBidPrice(0);
		case WAITING_PRICE -> operation.isBuy() ? tick.getBidPrice(0) : tick.getAskPrice(0);
		default -> throw new IllegalArgumentException("Unexpected value: " + priceType);
		};
		if(mlog.isInfoEnabled()) {
			mlog.info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
					tick.getActionDay(), LocalTime.parse(tick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER),
					contract.getUnifiedSymbol(), operation.text(), orderPrice, volume, priceType);
		}
		if(!gatewayMap.containsKey(contract)) {
			throw new NoSuchElementException(String.format("找不到合约 [%s] 对应网关", contract.getUnifiedSymbol()));
		}
		String id = UUID.randomUUID().toString();
		String gatewayId = gatewayMap.get(contract).gatewayId();
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
		return Optional.of(submitOrderReq(SubmitOrderReqField.newBuilder()
				.setOriginOrderId(id)
				.setContract(contract)
				.setGatewayId(gatewayId)
				.setDirection(OrderUtils.resolveDirection(operation))
				.setOffsetFlag(closingStrategy.resolveOperation(operation, pf))
				.setPrice(orderPrice)
				.setVolume(volume)		//	当信号交易量大于零时，优先使用信号交易量
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.setOrderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setActionTimestamp(latestTickMap.get(contract.getUnifiedSymbol()).getActionTimestamp())
				.setMinVolume(1)
				.build()));
	}
	
	@Override
	public synchronized void submitOrderReq(TradeIntent tradeIntent) {
		if(!module.isEnabled()) {
			mlog.info("策略处于停用状态，忽略委托单");
			return;
		}
		mlog.info("收到下单意图：{}", tradeIntent);
		this.tradeIntent = tradeIntent;
		tradeIntent.setContext(this);
		TickField tick = latestTickMap.get(tradeIntent.getContract().getUnifiedSymbol());
		Assert.notNull(tick, "没有行情时不应该发送订单");
		Assert.isTrue(tradeIntent.getVolume() > 0, "下单手数应该为正数");
		tradeIntent.onTick(tick);
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
		orderReqMap.put(orderReq.getOriginOrderId(), orderReq);
		accStore.onSubmitOrder(orderReq);
		gateway.submitOrder(orderReq);
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
	public synchronized IDisposablePriceListener priceTriggerOut(String unifiedSymbol, DirectionEnum openDir, DisposablePriceListenerType listenerType, double basePrice, int numOfPriceTickToTrigger, int volume) {
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
	public synchronized IDisposablePriceListener priceTriggerOut(TradeField trade, DisposablePriceListenerType listenerType, int numOfPriceTickToTrigger) {
		return priceTriggerOut(trade.getContract().getUnifiedSymbol(), trade.getDirection(), listenerType, trade.getPrice(), numOfPriceTickToTrigger, trade.getVolume());
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
				.setGatewayId(gateway.gatewayId())
				.setOriginOrderId(originOrderId)
				.build();
		accStore.onCancelOrder(cancelReq);
		gateway.cancelOrder(cancelReq);
	}
	
	@Override
	public int numOfMinPerModuleBar() {
		return numOfMinsPerBar;
	}
	
	@Override
	public synchronized int holdingNetProfit() {
		return gatewayMap.values().stream()
				.map(TradeGateway::gatewayId)
				.map(gatewayId -> accStore.getPositions(gatewayId))
				.flatMap(Collection::stream)
				.mapToInt(pf -> (int)pf.getPositionProfit())
				.sum();
	}
	
	@Override
	public synchronized int availablePosition(DirectionEnum direction, String unifiedSymbol) {
		return gatewayMap.values().stream()
				.map(TradeGateway::gatewayId)
				.map(gatewayId -> accStore.getPositions(gatewayId))
				.flatMap(Collection::stream)
				.filter(pf -> StringUtils.equals(pf.getContract().getUnifiedSymbol(), unifiedSymbol))
				.filter(pf -> FieldUtils.isLong(pf.getPositionDirection()) && FieldUtils.isBuy(direction) || FieldUtils.isShort(pf.getPositionDirection()) && FieldUtils.isSell(direction))
				.mapToInt(pf -> pf.getPosition() - pf.getFrozen())
				.sum();
	}
	
	@Override
	public synchronized int availablePosition(DirectionEnum direction, String unifiedSymbol, boolean isToday) {
		Stream<PositionField> posStream = gatewayMap.values().stream()
				.map(TradeGateway::gatewayId)
				.map(gatewayId -> accStore.getPositions(gatewayId))
				.flatMap(Collection::stream)
				.filter(pf -> StringUtils.equals(pf.getContract().getUnifiedSymbol(), unifiedSymbol))
				.filter(pf -> FieldUtils.isLong(pf.getPositionDirection()) && FieldUtils.isBuy(direction) || FieldUtils.isShort(pf.getPositionDirection()) && FieldUtils.isSell(direction));
		
		if(isToday)	return posStream.mapToInt(pf -> pf.getTdPosition() - pf.getTdFrozen()).sum();
		return posStream.mapToInt(pf -> pf.getYdPosition() - pf.getYdFrozen()).sum(); 
	}

	/* 此处收到的TICK数据是所有订阅的数据，需要过滤 */
	@Override
	public synchronized void onTick(TickField tick) {
		if(!bindedSymbolSet.contains(tick.getUnifiedSymbol())) {
			return;
		}
		mlog.trace("TICK信息: {} {} {} {}，最新价: {}", 
				tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), tick.getActionTimestamp(), tick.getLastPrice());
		if(Objects.nonNull(tradeIntent) && !tradeIntent.hasTerminated()) {
			tradeIntent.onTick(tick);
		}
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
		}
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
	
	/* 此处收到的BAR数据是所有订阅的数据，需要过滤 */
	@Override
	public synchronized void onBar(BarField bar) {
		if(!bindedSymbolSet.contains(bar.getUnifiedSymbol())) {
			return;
		}
		mlog.trace("分钟Bar信息: {} {} {} {}，最新价: {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getActionTimestamp(), bar.getClosePrice());
		indicatorFactory.getIndicatorMap().entrySet().stream().forEach(e -> e.getValue().onBar(bar));	// 普通指标的更新
		comboIndicators.stream().forEach(combo -> combo.onBar(bar));
		inspectedValIndicatorFactory.getIndicatorMap().entrySet().stream().forEach(e -> e.getValue().onBar(bar));	// 值透视指标的更新
		registry.onBar(bar);
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
		onRuntimeChangeCallback.accept(getRuntimeDescription(false));
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
		if(Objects.nonNull(tradeIntent)) {
			tradeIntent.onOrder(order);
		}
	}

	/* 此处收到的TRADE数据是所有成交回报，需要过滤 */
	@Override
	public synchronized void onTrade(TradeField trade) {
		if(!orderReqMap.containsKey(trade.getOriginOrderId()) && !StringUtils.equals(trade.getOriginOrderId(), Constants.MOCK_ORDER_ID)) {
			return;
		}
		if(orderReqMap.containsKey(trade.getOriginOrderId())) {
			if(mlog.isInfoEnabled()) {				
				mlog.info("成交：{}， 操作：{}{}， 价格：{}， 手数：{}", trade.getOriginOrderId(), FieldUtils.chn(trade.getDirection()), 
						FieldUtils.chn(trade.getOffsetFlag()), trade.getPrice(), trade.getVolume());
			}
			orderReqMap.remove(trade.getOriginOrderId());
		}
		accStore.onTrade(trade);
		tradeStrategy.onTrade(trade);
		onTradeNotification(trade);
		onRuntimeChangeCallback.accept(getRuntimeDescription(false));
		dealCollector.onTrade(trade).ifPresent(list -> list.stream().forEach(this.onDealCallback::accept));
		
		if(getState().isEmpty() && !listenerSet.isEmpty()) {
			mlog.info("净持仓为零，止盈止损监听器被清除");
			listenerSet.clear();
		}
		if(Objects.nonNull(tradeIntent)) {
			tradeIntent.onTrade(trade);
			if(tradeIntent.hasTerminated()) {
				tradeIntent = null;
			}
		}
	}
	
	private void onTradeNotification(TradeField trade) {
		StringBuilder sb = new StringBuilder();
		sb.append("模组成交:\n");
		sb.append(String.format(" 模组：%s%n", getModuleName()));
		sb.append(String.format(" 合约：%s%n", trade.getContract().getFullName()));
		sb.append(String.format(" 操作：%s%n", FieldUtils.chn(trade.getDirection()) + FieldUtils.chn(trade.getOffsetFlag())));
		sb.append(String.format(" 成交价：%s%n", trade.getPrice()));
		sb.append(String.format(" 手数：%s%n", trade.getVolume()));
		sb.append(String.format(" 成交时间：%s%n", trade.getTradeTime()));
		sb.append(String.format(" 委托ID：%s%n", trade.getOriginOrderId()));
		sendNotification(sb.toString());
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
	public synchronized void disabledModule() {
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
	public ContractField getContract(String unifiedSymbol) {
		if(!contractMap.containsKey(unifiedSymbol)) {
			throw new NoSuchElementException("找不到合约：" + unifiedSymbol);
		}
		return contractMap.get(unifiedSymbol);
	}

	@Override
	public ModuleState getState() {
		if(mlog.isTraceEnabled()) {			
			mlog.trace("当前状态：{}", accStore.getModuleState());
		}
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
	public boolean explain(boolean expression, String infoMessage, Object... args) {
		if(expression) {
			mlog.info(infoMessage, args);
		}
		return expression;
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
	public void sendNotification(String content) {
		mailMgr.onEvent(new NorthstarEvent(NorthstarEventType.NOTICE, NoticeField.newBuilder()
				.setTimestamp(System.currentTimeMillis())
				.setContent(content)
				.setStatus(CommonStatusEnum.COMS_INFO)
				.build()));
	}

}
