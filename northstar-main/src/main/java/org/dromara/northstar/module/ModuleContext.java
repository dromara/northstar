package org.dromara.northstar.module;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.constant.ModuleUsage;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.exception.InsufficientException;
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
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.indicator.IndicatorValueUpdateHelper;
import org.dromara.northstar.indicator.constant.PeriodUnit;
import org.dromara.northstar.indicator.model.Configuration;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IMessageSender;
import org.dromara.northstar.strategy.IModule;
import org.dromara.northstar.strategy.IModuleAccount;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.OrderRequestFilter;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.support.log.ModuleLoggerFactory;
import org.dromara.northstar.support.notification.IMessageSenderManager;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry.ListenerType;
import org.slf4j.Logger;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

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
	
	protected IModuleRepository moduleRepo;
	
	protected ModuleAccount moduleAccount;
	
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
	
	protected Set<IndicatorValueUpdateHelper> indicatorHelperSet = new HashSet<>();
	
	protected TradeIntent tradeIntent;	// 交易意图
	
	protected final AtomicInteger bufSize = new AtomicInteger(0);
	
	protected final BarMergerRegistry registry = new BarMergerRegistry();
	
	protected boolean enabled;
	
	protected String tradingDay = "";
	
	protected IContractManager contractMgr;
	
	protected OrderRequestFilter orderReqFilter;
	
	public ModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription, ModuleRuntimeDescription moduleRtDescription,
			IContractManager contractMgr, IModuleRepository moduleRepo, ModuleLoggerFactory loggerFactory, IMessageSenderManager senderMgr) {
		this.tradeStrategy = tradeStrategy;
		this.moduleRepo = moduleRepo;
		this.contractMgr = contractMgr;
		this.logger = loggerFactory.getLogger(moduleDescription.getModuleName());
		this.senderMgr = senderMgr;
		this.bufSize.set(moduleDescription.getModuleCacheDataSize());
		this.moduleAccount = new ModuleAccount(moduleDescription, moduleRtDescription, new ModuleStateMachine(this), moduleRepo, contractMgr, logger);
		moduleDescription.getModuleAccountSettingsDescription().stream()
			.forEach(mad -> {
				for(ContractSimpleInfo csi : mad.getBindedContracts()) {
					Contract contract = contractMgr.getContract(Identifier.of(csi.getValue()));
					ContractField cf = contract.contractField();
					contractMap.put(csi.getUnifiedSymbol(), cf);
					contractMap2.put(cf, contract);
					barBufQMap.put(cf.getUnifiedSymbol(), new LinkedList<>());
					registry.addListener(contract, moduleDescription.getNumOfMinPerBar(), PeriodUnit.MINUTE, tradeStrategy, ListenerType.STRATEGY);
					registry.addListener(contract, moduleDescription.getNumOfMinPerBar(), PeriodUnit.MINUTE, this, ListenerType.CONTEXT);
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
	public synchronized void submitOrderReq(TradeIntent tradeIntent) {
		if(!module.isEnabled()) {
			getLogger().info("策略处于停用状态，忽略委托单");
			return;
		}
		TickField tick = latestTickMap.get(tradeIntent.getContract().getUnifiedSymbol());
		if(Objects.isNull(tick)) {
			getLogger().warn("没有TICK行情数据时，忽略下单请求");
			return;
		}
		getLogger().info("收到下单意图：{}", tradeIntent);
		this.tradeIntent = tradeIntent;
		tradeIntent.setContext(this);
        tradeIntent.onTick(tick);
	}

	@Override
	public int numOfMinPerMergedBar() {
		return module.getModuleDescription().getNumOfMinPerBar();
	}

	@Override
	public IAccount getAccount(ContractField contract) {
		Contract c = contractMap2.get(contract);
		return module.getAccount(c);
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
	public void registerIndicator(Indicator indicator) {
		checkIndicator(indicator);
		Configuration cfg = indicator.getConfiguration();
		Contract c = contractMap2.get(cfg.contract());
		IndicatorValueUpdateHelper helper = new IndicatorValueUpdateHelper(indicator);
		indicatorHelperSet.add(helper);
		registry.addListener(c, cfg.numOfUnits(), cfg.period(), helper, ListenerType.INDICATOR);
	}
	
	public void checkIndicator(Indicator indicator) {
		// 递归子指标
		for(Indicator in : indicator.dependencies()) {
			checkIndicator(in);
		}
		Configuration cfg = indicator.getConfiguration();
		Assert.isTrue(cfg.numOfUnits() > 0, "周期数必须大于0，当前为：" + cfg.numOfUnits());
		Assert.isTrue(cfg.cacheLength() > 0, "指标回溯长度必须大于0，当前为：" + cfg.cacheLength());
		indicatorValBufQMap.put(indicator, new LinkedList<>());
	}
	
	@Override
	public IMessageSender getMessageSender() {
		return senderMgr.getSender();
	}

	@Override
	public synchronized void onTick(TickField tick) {
		getLogger().trace("TICK信息: {} {} {} {}，最新价: {}", 
				tick.getUnifiedSymbol(), tick.getActionDay(), tick.getActionTime(), tick.getActionTimestamp(), tick.getLastPrice());
		if(Objects.nonNull(tradeIntent)) {
			tradeIntent.onTick(tick);
			if(tradeIntent.hasTerminated()) 
				tradeIntent = null;
		}
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
		}
		indicatorHelperSet.forEach(helper -> helper.onTick(tick));
		moduleAccount.onTick(tick);
		latestTickMap.put(tick.getUnifiedSymbol(), tick);
		tradeStrategy.onTick(tick);
	}

	@Override
	public synchronized void onBar(BarField bar) {
		getLogger().trace("分钟Bar信息: {} {} {} {}，最新价: {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getActionTimestamp(), bar.getClosePrice());
		indicatorHelperSet.forEach(helper -> helper.onBar(bar));
		registry.onBar(bar);		
	}
	
	@Override
	public synchronized void onMergedBar(BarField bar) {
		getLogger().debug("合并Bar信息: {} {} {} {}，最新价: {}", bar.getUnifiedSymbol(), bar.getActionDay(), bar.getActionTime(), bar.getActionTimestamp(), bar.getClosePrice());
		try {			
			indicatorHelperSet.stream().map(IndicatorValueUpdateHelper::getIndicator).forEach(indicator -> visualize(indicator, bar));
		} catch(Exception e) {
			getLogger().error(e.getMessage(), e);
		}
		if(barBufQMap.get(bar.getUnifiedSymbol()).size() >= bufSize.intValue()) {
			barBufQMap.get(bar.getUnifiedSymbol()).poll();
		}
		barBufQMap.get(bar.getUnifiedSymbol()).offer(bar);		
		if(isEnabled()) {
			moduleRepo.saveRuntime(getRuntimeDescription(false));
		}
	}
	
	private void visualize(Indicator indicator, BarField bar) {
		for(Indicator in : indicator.dependencies()) {
			visualize(in, bar);
		}
		if(!StringUtils.equals(indicator.getConfiguration().contract().getUnifiedSymbol(), bar.getUnifiedSymbol())) {
			return;
		}
		LinkedList<TimeSeriesValue> list = (LinkedList<TimeSeriesValue>) indicatorValBufQMap.get(indicator);
		if(list.size() >= bufSize.intValue()) {
			list.poll();
		}
		if(indicator.isReady() && indicator.getConfiguration().visible() && indicator.get(0).timestamp() == bar.getActionTimestamp()
				&& (list.isEmpty() || list.peekLast().getTimestamp() != bar.getActionTimestamp())
				&& (BarUtils.isEndOfTheTradingDay(bar) || indicator.getConfiguration().ifPlotPerBar() || !indicator.get(0).unstable())) {		
			list.offer(new TimeSeriesValue(indicator.get(0).value(), bar.getActionTimestamp()));	
		}
	}

	@Override
	public synchronized void onOrder(OrderField order) {
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
	public synchronized void onTrade(TradeField trade) {
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
		
		if(Objects.nonNull(tradeIntent)) {
			tradeIntent.onTrade(trade);
			if(tradeIntent.hasTerminated()) 
				tradeIntent = null;
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
	public synchronized ModuleRuntimeDescription getRuntimeDescription(boolean fullDescription) {
		Map<String, ModuleAccountRuntimeDescription> accMap = new HashMap<>();
		module.getModuleDescription().getModuleAccountSettingsDescription().forEach(mad -> {
			String gatewayId = module.getModuleDescription().getUsage() == ModuleUsage.PLAYBACK 
					? PlaybackModuleContext.PLAYBACK_GATEWAY 
					: mad.getAccountGatewayId();
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
				.dataState(tradeStrategy.getStoreObject())
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
				String unifiedSymbol = in.getConfiguration().contract().getUnifiedSymbol();
				Configuration cfg = in.getConfiguration();
				String indicatorName = String.format("%s_%d%s", cfg.indicatorName(), cfg.numOfUnits(), cfg.period().symbol());
				if(!indicatorMap.containsKey(unifiedSymbol)) {
					indicatorMap.put(unifiedSymbol, new ArrayList<>());
				}
				if(cfg.visible()) {
					indicatorMap.get(unifiedSymbol).add(indicatorName);
				}
				Collections.sort(indicatorMap.get(unifiedSymbol));
				
				e.getValue().stream().forEach(tv -> {
					if(!symbolTimeObject.containsKey(unifiedSymbol)
							|| !symbolTimeObject.get(unifiedSymbol).containsKey(tv.getTimestamp())) {
						return;
					}
					symbolTimeObject.get(unifiedSymbol).get(tv.getTimestamp()).put(indicatorName, tv.getValue());
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
		setOrderRequestFilter(new DefaultOrderFilter(module));
	}

	@Override
	public synchronized Optional<String> submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume, double price) {
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
		String gatewayId = getAccount(contract).accountId();
		DirectionEnum direction = OrderUtils.resolveDirection(operation);
		List<TradeField> nonclosedTrades = moduleAccount.getNonclosedTrades(contract.getUnifiedSymbol(), FieldUtils.getOpposite(direction));
		return Optional.ofNullable(submitOrderReq(SubmitOrderReqField.newBuilder()
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
	
	private String submitOrderReq(SubmitOrderReqField orderReq) {
		if(getLogger().isInfoEnabled()) {			
			getLogger().info("发单：{}，{}", orderReq.getOriginOrderId(), LocalDateTime.ofInstant(Instant.ofEpochMilli(orderReq.getActionTimestamp()), ZoneId.systemDefault()));
		}
		try {
			moduleAccount.onSubmitOrder(orderReq);
		} catch (InsufficientException e) {
			getLogger().error("发单失败。原因：" + e.getMessage(), e);
			return null;
		}
		try {
			if(Objects.nonNull(orderReqFilter)) {
				orderReqFilter.doFilter(orderReq);
			}
		} catch (Exception e) {
			getLogger().error("发单失败。原因：" + e.getMessage(), e);
			return null;
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
	public synchronized void cancelOrder(String originOrderId) {
		if(!orderReqMap.containsKey(originOrderId)) {
			getLogger().debug("找不到订单：{}", originOrderId);
			return;
		}
		getLogger().info("撤单：{}", originOrderId);
		ContractField contract = orderReqMap.get(originOrderId).getContract();
		Contract c = contractMgr.getContract(Identifier.of(contract.getContractId()));
		CancelOrderReqField cancelReq = CancelOrderReqField.newBuilder()
				.setGatewayId(contract.getGatewayId())
				.setOriginOrderId(originOrderId)
				.build();
		moduleAccount.onCancelOrder();
		module.getAccount(c).cancelOrder(cancelReq);
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

}
