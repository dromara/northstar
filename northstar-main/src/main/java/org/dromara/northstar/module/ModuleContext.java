package org.dromara.northstar.module;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.dromara.northstar.common.constant.ChannelType;
import org.dromara.northstar.common.constant.Constants;
import org.dromara.northstar.common.constant.Interval;
import org.dromara.northstar.common.constant.ModuleState;
import org.dromara.northstar.common.model.ContractChartDescription;
import org.dromara.northstar.common.model.ContractSimpleInfo;
import org.dromara.northstar.common.model.Identifier;
import org.dromara.northstar.common.model.ModuleChartDescription;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.ModuleLoggingFactory;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.mktdata.MarketCenter;
import org.dromara.northstar.indicator.Indicator;
import org.dromara.northstar.strategy.IAccount;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.MergedBarListener;
import org.dromara.northstar.strategy.OrderRequestFilter;
import org.dromara.northstar.strategy.TradeStrategy;
import org.slf4j.Logger;
import org.springframework.util.Assert;

public class ModuleContext implements IModuleContext{
	
	private final ModuleDescription moduleDescription;
	
	private final Logger logger;
	
	protected TradeStrategy tradeStrategy;
	
	protected IModuleRepository moduleRepo;
	
	protected ModuleAccount moduleAccount;
	
	/* originOrderId -> orderReq */
	protected ConcurrentMap<String, SubmitOrderReq> orderReqMap = new ConcurrentHashMap<>();
	
	protected EnumMap<Interval, TimeFrameScopedRegistry> timeFrameScoped = new EnumMap<>(Interval.class);

	protected boolean enabled;
	
	protected LocalDate tradingDay;
	
	protected MarketCenter mktCenter;
	
	protected OrderRequestFilter orderReqFilter;
	
	protected ModuleStateMachine moduleStateMachine;
	
	public ModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription, ModuleAccount moduleAcc, MarketCenter marketCenter, 
			IModuleRepository moduleRepo) {
		this.moduleDescription = moduleDescription;
		this.tradeStrategy = tradeStrategy;
		this.moduleRepo = moduleRepo;
		this.mktCenter = marketCenter;
		this.logger = getLogger(getClass());
		this.moduleStateMachine = new ModuleStateMachine();
		this.orderReqFilter = new DefaultOrderFilter(moduleDescription.getBindedContracts(), this);
		
		for(Interval barInterval : Interval.values()) {
			timeFrameScoped.put(barInterval, new TimeFrameScopedRegistry(barInterval, this));
		}
		
		this.moduleAccount = moduleAcc;
		
		String name = moduleDescription.getName();
		ModuleLoggingFactory.register(name, moduleStateMachine);
		ModuleLoggingFactory.register(name, moduleAcc);
		
		moduleStateMachine.setModuleAccount(moduleAccount);
	}
	
	@Override
	public Contract getContract(String unifiedSymbol) {
		Assert.hasText(unifiedSymbol, "合约编码不能为空");
		ContractSimpleInfo contractSimple = getModuleDescription().getBindedContracts()
			.stream()
			.filter(csi -> csi.getUnifiedSymbol().equals(unifiedSymbol))
			.findAny()
			.get();
		return mktCenter.getContract(Identifier.of(contractSimple.getId())).contract();
	}

	@Override
	public void submitOrderReq(SubmitOrderReq orderReq) {
		if(!isEnabled()) {
			logger.info("策略处于停用状态，忽略委托单");
			return;
		}

		logger.info("准备发单：{}，{} {} {}", orderReq.contract().name(), orderReq.originOrderId(), orderReq.direction(), orderReq.offsetFlag());
		try {
			if(Objects.nonNull(orderReqFilter)) {
				orderReqFilter.doFilter(orderReq);
			}
			moduleAccount.submitOrder(orderReq);
			moduleStateMachine.onSubmitReq();
			orderReqMap.put(orderReq.originOrderId(), orderReq);
		} catch (Exception e) {
			logger.error("发单失败。原因：{}", e.getMessage());
			moduleStateMachine.onFailSubmitReq();
			logger.warn("发单异常，主动停用模组");
			setEnabled(false);
		}
	}
	
	@Override
	public void cancelOrder(CancelOrderReq cancelReq) {
		if(!orderReqMap.containsKey(cancelReq.originOrderId())) {
			logger.warn("找不到订单：{}", cancelReq.originOrderId());
			return;
		}
		if(!getState().isOrdering()) {
			logger.info("非下单状态，忽略撤单请求：{}", cancelReq.originOrderId());
			return;
		}
		logger.info("撤单：{}", cancelReq.originOrderId());
		if(moduleAccount.cancelOrder(cancelReq)) {
			orderReqMap.remove(cancelReq.originOrderId());
		}
	}

	@Override
	public IAccount getAccount(Contract contract) {
		return moduleAccount.getContractAccountMap().get(contract);
	}

	@Override
	public IAccount getModuleAccount() {
		return moduleAccount;
	}

	@Override
	public ModuleState getState() {
		return moduleStateMachine.getState();
	}

	@Override
	public void disabledModule() {
		logger.warn("策略层主动停用模组");
		setEnabled(false);
	}

	@Override
	public Logger getLogger(Class<?> clz) {
		return ModuleLoggingFactory.getLogger(moduleDescription.getName(), clz.getName());
	}
	
	@Override
	public void registerIndicator(Indicator indicator) {
		timeFrameScoped.get(indicator.getConfiguration().interval()).registerIndicator(indicator);
	}
	
	@Override
	public void onTick(Tick tick) {
		logger.trace("TICK数据: {} {} {} {}，最新价：{}，累计成交：{}，成交量：{}，累计持仓：{}，持仓量：{}", 
				tick.contract().unifiedSymbol(), tick.actionDay(), tick.actionTime(), tick.actionTimestamp(),
				tick.lastPrice(), tick.volume(), tick.volumeDelta(), tick.openInterest(), tick.openInterestDelta());
		if(!Objects.equals(tick.tradingDay(), tradingDay)) {
			tradingDay = tick.tradingDay();
		}
		
		tradeStrategy.onTick(tick);
	}

	@Override
	public void onBar(Bar bar) {
		logger.debug("Bar数据: {} {} {} {} {} unstable:{}", bar.contract().name(), bar.interval(), bar.actionDay(), bar.actionTime(), bar.actionTimestamp(), bar.unstable());
		timeFrameScoped.get(bar.interval()).onBar(bar);
	}
	
	@Override
	public void onMergedBar(Bar bar) {
		if(bar.channelType() == ChannelType.PLAYBACK || bar.channelType() == ChannelType.SIM
				|| bar.interval() != Interval.M1 || !isEnabled()) {
			return;
		}
		
		moduleRepo.saveRuntime(getRuntimeDescription());
	}
	
	@Override
	public void onOrder(Order order) {
		if(!orderReqMap.containsKey(order.originOrderId())) {
			return;
		}
		logger.info("收到订单反馈：{} 合约：{} 订单状态：{} {}", order.originOrderId(), order.contract().name(), order.orderStatus(), order.statusMsg());
		if(!OrderUtils.isValidOrder(order) || OrderUtils.isDoneOrder(order)) {
			// 延时3秒再移除订单信息，避免移除了订单信息后，成交无法匹配的问题
			CompletableFuture.runAsync(() -> orderReqMap.remove(order.originOrderId()), CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS));
		}
		moduleAccount.onOrder(order);
		moduleStateMachine.onOrder(order);
		tradeStrategy.onOrder(order);
	}

	@Override
	public void onTrade(Trade trade) {
		if(!orderReqMap.containsKey(trade.originOrderId()) && !StringUtils.equals(trade.originOrderId(), Constants.MOCK_ORDER_ID)) {
			return;
		} 
		if(logger.isInfoEnabled()) {
			logger.info("收到成交反馈：{}， 操作：{}{}，合约：{}，价格：{}， 交易量：{}", trade.originOrderId(), FieldUtils.chn(trade.direction()),
					FieldUtils.chn(trade.offsetFlag()), trade.contract().name(), trade.price(), trade.volume());
		}
		moduleAccount.onTrade(trade);
		moduleStateMachine.onTrade(trade);
		tradeStrategy.onTrade(trade);
		moduleRepo.saveRuntime(getRuntimeDescription());
	}

	@Override
	public ModuleRuntimeDescription getRuntimeDescription() {
		return ModuleRuntimeDescription.builder()
				.moduleName(moduleDescription.getName())
				.storeObject(tradeStrategy.getStoreObject())
				.build();
	}

	
	@Override
	public ModuleChartDescription getChartDescription() {
		Map<Interval, List<ContractChartDescription>> dataMap = new EnumMap<>(Interval.class);
		for(Entry<Interval, TimeFrameScopedRegistry> entry : timeFrameScoped.entrySet()) {
			dataMap.put(entry.getKey(), entry.getValue().visulize());
		}
		
		return ModuleChartDescription.builder()
				.data(dataMap)
				.strategyInfos(tradeStrategy.strategyInfos())
				.build();
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		logger.info("【{}】 模组", enabled ? "启用" : "停用");
		this.enabled = enabled;
		moduleRepo.saveRuntime(getRuntimeDescription());
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setOrderRequestFilter(OrderRequestFilter filter) {
		this.orderReqFilter = filter;
	}

	@Override
	public int getDefaultVolume() {
		return moduleDescription.getDefaultVolume();
	}

	@Override
	public TradeStrategy getStrategy() {
		return tradeStrategy;
	}

	@Override
	public void addMergedBarListener(Contract contract, Interval interval, MergedBarListener listener) {
		timeFrameScoped.get(interval).registerBarListener(contract, listener);
	}

	@Override
	public List<Contract> bindedContracts() {
		return moduleDescription.getBindedContracts()
				.stream()
				.map(csi -> getContract(csi.getUnifiedSymbol()))
				.toList();
	}

	@Override
	public ModuleDescription getModuleDescription() {
		return moduleDescription;
	}

}
