package org.dromara.northstar.module;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
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

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.model.BarWrapper;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleAccountRuntimeDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModulePositionDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.TimeSeriesValue;
import org.dromara.northstar.common.utils.BarUtils;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.Contract;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.indicator.IndicatorFactory;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IComboIndicator;
import org.dromara.northstar.strategy.IDisposablePriceListener;
import org.dromara.northstar.strategy.IMessageSender;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleAccount;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.OrderRequestFilter;
import org.dromara.northstar.strategy.TimeSeriesUnaryOperator;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.DisposablePriceListenerType;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.Indicator;
import org.dromara.northstar.strategy.model.Indicator.Configuration;
import org.dromara.northstar.strategy.model.Indicator.PeriodUnit;
import org.dromara.northstar.strategy.model.Indicator.ValueType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.support.log.ModuleLoggerFactory;
import org.dromara.northstar.support.notification.IMessageSenderManager;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry.ListenerType;
import org.slf4j.Logger;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.util.concurrent.AtomicDouble;

import cn.hutool.core.lang.Assert;
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

public class ModuleContext implements IModuleContext{
	
	protected IModule module;
	
	protected Logger logger;
	
	protected IMessageSenderManager senderMgr;
	
	protected TradeStrategy tradeStrategy;
	
	protected Set<DisposablePriceListener> listenerSet = new HashSet<>();
	
	protected IModuleRepository moduleRepo;
	
	protected IModuleAccount moduleAccount;
	
	/* originOrderId -> orderReq */
	private Map<String, SubmitOrderReqField> orderReqMap = new HashMap<>();
	
	/* unifiedSymbol -> contract */
	protected Map<String, ContractField> contractMap = new HashMap<>();
	protected Map<ContractField, Contract> contractMap2 = new HashMap<>();
	
	/* unifiedSymbol -> tick */
	protected Map<String, TickField> latestTickMap = new HashMap<>();
	
	/* unifiedSymbol -> barQ */
	protected Map<String, Queue<BarField>> barBufQMap = new HashMap<>();
	
	/* indicator -> values */
	protected Map<Indicator, Queue<TimeSeriesValue>> indicatorValBufQMap = new HashMap<>(); 
	
	protected TradeIntent tradeIntent;	// 交易意图
	
	protected IndicatorFactory indicatorFactory;
	
	protected IndicatorFactory inspectedValIndicatorFactory;
	
	protected final HashSet<IComboIndicator> comboIndicators = new HashSet<>();
	
	protected final AtomicInteger bufSize = new AtomicInteger(0);
	
	protected final BarMergerRegistry registry = new BarMergerRegistry();
	
	protected boolean enabled;
	
	protected String tradingDay = "";
	
	public ModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRtDescription,
			IContractManager contractMgr, IModuleRepository moduleRepo, ModuleLoggerFactory loggerFactory, IMessageSenderManager senderMgr) {
		this.tradeStrategy = tradeStrategy;
		this.moduleRepo = moduleRepo;
		this.logger = loggerFactory.getLogger(moduleDescription.getModuleName());
		this.senderMgr = senderMgr;
		this.moduleAccount = new ModuleAccount(moduleDescription, moduleRtDescription, new ModuleStateMachine(this), moduleRepo, contractMgr, logger);
		moduleDescription.getModuleAccountSettingsDescription().stream()
			.forEach(mad -> {
				for(ContractSimpleInfo csi : mad.getBindedContracts()) {
					Contract contract = contractMgr.getContract(Identifier.of(csi.getValue()));
					ContractField cf = contract.contractField();
					contractMap.put(csi.getUnifiedSymbol(), cf);
					contractMap2.put(cf, contract);
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
	public ContractField getContract(String unifiedSymbol) {
		return contractMap.get(unifiedSymbol);
	}

	@Override
	public void submitOrderReq(TradeIntent tradeIntent) {
		if(!module.isEnabled()) {
			getLogger().info("策略处于停用状态，忽略委托单");
			return;
		}
		getLogger().info("收到下单意图：{}", tradeIntent);
		this.tradeIntent = tradeIntent;
		tradeIntent.setContext(this);
		TickField tick = latestTickMap.get(tradeIntent.getContract().getUnifiedSymbol());
		Assert.notNull(tick, "没有行情时不应该发送订单");
		Assert.isTrue(tradeIntent.getVolume() > 0, "下单手数应该为正数");
		tradeIntent.onTick(tick);
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
		if(getLogger().isInfoEnabled())
			getLogger().info("增加【{}】", listener.description());
		listenerSet.add(listener);
		return listener;
	}

	@Override
	public IDisposablePriceListener priceTriggerOut(TradeField trade, DisposablePriceListenerType listenerType,
			int numOfPriceTickToTrigger) {
		return priceTriggerOut(trade.getContract().getUnifiedSymbol(), trade.getDirection(), listenerType, trade.getPrice(), numOfPriceTickToTrigger, trade.getVolume());
	}

	@Override
	public int numOfMinPerMergedBar() {
		return module.getModuleDescription().getNumOfMinPerBar();
	}

	@Override
	public IAccount getAccount(ContractField contract) {
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
		setEnabled(false);
	}

	@Override
	public Logger getLogger() {
		return logger;
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
	public IMessageSender getMessageSender() {
		return senderMgr.getSender();
	}

	@Override
	public void onTick(TickField tick) {
		getLogger().trace("TICK信息: {} {} {} {}，最新价: {}", 
				tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), tick.getActionTimestamp(), tick.getLastPrice());
		if(Objects.nonNull(tradeIntent) && !tradeIntent.hasTerminated()) {
			tradeIntent.onTick(tick);
		}
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
		}
		indicatorFactory.getIndicatorMap().values().stream().forEach(indicator -> indicator.onTick(tick));
		comboIndicators.stream().forEach(combo -> combo.onTick(tick));
		moduleAccount.onTick(tick);
		latestTickMap.put(tick.getUnifiedSymbol(), tick);
		listenerSet.stream()
			.filter(listener -> listener.shouldBeTriggered(tick))
			.forEach(listener -> {
				getLogger().info("触发【{}】", listener.description());
				listener.execute();
			});
		tradeStrategy.onTick(tick);
		listenerSet = listenerSet.stream().filter(DisposablePriceListener::isValid).collect(Collectors.toSet());
	}

	@Override
	public void onBar(BarField bar) {
		getLogger().trace("分钟Bar信息: {} {} {} {}，最新价: {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getActionTimestamp(), bar.getClosePrice());
		indicatorFactory.getIndicatorMap().entrySet().stream().forEach(e -> e.getValue().onBar(bar));	// 普通指标的更新
		comboIndicators.stream().forEach(combo -> combo.onBar(bar));
		inspectedValIndicatorFactory.getIndicatorMap().entrySet().stream().forEach(e -> e.getValue().onBar(bar));	// 值透视指标的更新
		registry.onBar(bar);		
	}
	
	@Override
	public void onMergedBar(BarField bar) {
		getLogger().debug("合并Bar信息: {} {} {} {}，最新价: {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getActionTimestamp(), bar.getClosePrice());
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
		moduleRepo.saveRuntime(getRuntimeDescription(false));
	}

	@Override
	public void onOrder(OrderField order) {
		if(!orderReqMap.containsKey(order.getOriginOrderId())) {
			return;
		}
		if(!OrderUtils.isValidOrder(order)) {
			orderReqMap.remove(order.getOriginOrderId());
		}
		moduleAccount.onOrder(order);
		tradeStrategy.onOrder(order);
		if(Objects.nonNull(tradeIntent)) {
			tradeIntent.onOrder(order);
		}		
	}

	@Override
	public void onTrade(TradeField trade) {
		if(!orderReqMap.containsKey(trade.getOriginOrderId()) && !StringUtils.equals(trade.getOriginOrderId(), Constants.MOCK_ORDER_ID)) {
			return;
		}
		if(orderReqMap.containsKey(trade.getOriginOrderId())) {
			if(getLogger().isInfoEnabled()) {				
				getLogger().info("成交：{}， 操作：{}{}， 价格：{}， 手数：{}", trade.getOriginOrderId(), FieldUtils.chn(trade.getDirection()), 
						FieldUtils.chn(trade.getOffsetFlag()), trade.getPrice(), trade.getVolume());
			}
			orderReqMap.remove(trade.getOriginOrderId());
		}
		moduleAccount.onTrade(trade);
		tradeStrategy.onTrade(trade);
		moduleRepo.saveRuntime(getRuntimeDescription(false));
		
		if(getState().isEmpty() && !listenerSet.isEmpty()) {
			getLogger().info("净持仓为零，止盈止损监听器被清除");
			listenerSet.clear();
		}
		if(Objects.nonNull(tradeIntent)) {
			tradeIntent.onTrade(trade);
			if(tradeIntent.hasTerminated()) {
				tradeIntent = null;
			}
		}
	}

	@Override
	public void initData(List<BarField> barData) {
		if(barData.isEmpty()) {
			getLogger().debug("初始化数据为空");
			return;
		}
		
		getLogger().debug("合约{} 初始化数据 {} {} -> {} {}", barData.get(0).getUnifiedSymbol(),
				barData.get(0).getActionDay(), barData.get(0).getActionTime(), 
				barData.get(barData.size() - 1).getActionDay(), barData.get(barData.size() - 1).getActionTime());
		boolean flag = enabled;
		enabled = false;
		for(BarField bar : barData) {
			onBar(bar);
		}
		enabled = flag;
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
		Map<String, ModuleAccountRuntimeDescription> accMap = new HashMap<>();
		module.getModuleDescription().getModuleAccountSettingsDescription().forEach(mad -> {
			String gatewayId = mad.getAccountGatewayId();
			ModulePositionDescription posDescription = ModulePositionDescription.builder()
					.logicalPositions(moduleAccount.getPositions(gatewayId).stream().map(PositionField::toByteArray).toList())
					.nonclosedTrades(moduleAccount.getNonclosedTrades(gatewayId).stream().map(TradeField::toByteArray).toList())
					.build();
			
			ModuleAccountRuntimeDescription accDescription = ModuleAccountRuntimeDescription.builder()
					.accountId(gatewayId)
					.initBalance(moduleAccount.getInitBalance(gatewayId))
					.accCloseProfit(moduleAccount.getAccCloseProfit(gatewayId))
					.accDealVolume(moduleAccount.getAccDealVolume(gatewayId))
					.accCommission(moduleAccount.getAccCommission(gatewayId))
					.maxDrawBack(moduleAccount.getMaxDrawBack(gatewayId))
					.maxProfit(moduleAccount.getMaxProfit(gatewayId))
					.positionDescription(posDescription)
					.build();
			accMap.put(gatewayId, accDescription);
		});
		
		ModuleRuntimeDescription mad = ModuleRuntimeDescription.builder()
				.moduleName(module.getName())
				.enabled(module.isEnabled())
				.moduleState(moduleAccount.getModuleState())
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
	public void setModule(IModule module) {
		this.module = module;
		this.indicatorFactory = new IndicatorFactory(module);
		this.inspectedValIndicatorFactory = new IndicatorFactory(module);
		setOrderRequestFilter(new DefaultOrderFilter(module));
	}

	@Override
	public Optional<String> submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume, double price) {
		if(!module.isEnabled()) {
			getLogger().info("策略处于停用状态，忽略委托单");
			return Optional.empty();
		}
		TickField tick = latestTickMap.get(contract.getUnifiedSymbol());
		Assert.notNull(tick, "没有行情时不应该发送订单");
		Assert.isTrue(volume > 0, "下单手数应该为正数");
		
		double orderPrice = priceType.resolvePrice(tick, operation, price);
		if(getLogger().isInfoEnabled()) {
			getLogger().info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
					tick.getActionDay(), LocalTime.parse(tick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER),
					contract.getUnifiedSymbol(), operation.text(), orderPrice, volume, priceType);
		}
		String id = UUID.randomUUID().toString();
		String gatewayId = contract.getGatewayId();
		DirectionEnum direction = OrderUtils.resolveDirection(operation);
		List<TradeField> nonclosedTrades = moduleAccount.getNonclosedTrades(contract.getUnifiedSymbol(), FieldUtils.getOpposite(direction));
		return Optional.of(submitOrderReq(SubmitOrderReqField.newBuilder()
				.setOriginOrderId(id)
				.setContract(contract)
				.setGatewayId(gatewayId)
				.setDirection(direction)
				.setOffsetFlag(module.getModuleDescription().getClosingPolicy().resolveOffsetFlag(operation, contract, nonclosedTrades, tick.getTradingDay()))
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
	
	private DateFormat fmt = new SimpleDateFormat();
	
	private String submitOrderReq(SubmitOrderReqField orderReq) {
		if(getLogger().isInfoEnabled()) {			
			getLogger().info("发单：{}，{}", orderReq.getOriginOrderId(), fmt.format(new Date(orderReq.getActionTimestamp())));
		}
		try {
			moduleAccount.onSubmitOrder(orderReq);
		} catch (InsufficientException e) {
			throw new InsufficientException(String.format("模组 [%s] 下单失败，原因：%s", module.getName(), e.getMessage()));
		}
		ContractField contract = orderReq.getContract();
		String originOrderId = module.getAccount(contract).submitOrder(orderReq);
		orderReqMap.put(originOrderId, orderReq);
		return originOrderId;
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
		if(!orderReqMap.containsKey(originOrderId)) {
			getLogger().debug("找不到订单：{}", originOrderId);
			return;
		}
		getLogger().info("撤单：{}", originOrderId);
		ContractField contract = orderReqMap.get(originOrderId).getContract();
		CancelOrderReqField cancelReq = CancelOrderReqField.newBuilder()
				.setGatewayId(contract.getGatewayId())
				.setOriginOrderId(originOrderId)
				.build();
		moduleAccount.onCancelOrder(cancelReq);
		module.getAccount(contract).cancelOrder(cancelReq);
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		moduleRepo.saveRuntime(getRuntimeDescription(false));
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setOrderRequestFilter(OrderRequestFilter filter) {
		contractMap.values().forEach(c -> module.getAccount(c).setOrderRequestFilter(filter));
	}

}
