package org.dromara.northstar.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;

public class PlaybackModuleContext extends ModuleContext implements IModuleContext{
	
	public static final String PLAYBACK_GATEWAY = "回测账户";
	
	private Logger logger;
	
	private Optional<TradeIntent> pendingTrade = Optional.empty();
	
	private long signalTime;
	
	public PlaybackModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription, 
			ModuleRuntimeDescription moduleRtDescription, IContractManager contractMgr, IModuleRepository moduleRepo, BarMergerRegistry barMergerRegistry) {
		super(tradeStrategy, moduleDescription, moduleRtDescription, contractMgr, new MockModuleRepository(moduleRepo), barMergerRegistry);
		logger = getLogger(getClass());
	}

	@Override
	public synchronized void submitOrderReq(TradeIntent tradeIntent) {
		pendingTrade = Optional.of(tradeIntent);
		Tick lastTick = Optional.ofNullable(tickMap.get(tradeIntent.getContract())).orElseThrow(() -> new IllegalStateException("没有行情时不应该发送订单"));
		signalTime = lastTick.actionTimestamp();
	}
	
	private void doTrade() {
		TradeIntent tradeIntent = pendingTrade.get();
		// 回测时直接成交，没有撤单追单逻辑
		SignalOperation operation = tradeIntent.getOperation();
		if(operation == SignalOperation.BUY_REVERSE) {
			submitOrderReq(tradeIntent.getContract(), SignalOperation.BUY_CLOSE, tradeIntent.getPriceType(), tradeIntent.getVolume(), tradeIntent.getPrice());
			submitOrderReq(tradeIntent.getContract(), SignalOperation.BUY_OPEN, tradeIntent.getPriceType(), tradeIntent.getVolume(), tradeIntent.getPrice());
		} else if (operation == SignalOperation.SELL_REVERSE) {
			submitOrderReq(tradeIntent.getContract(), SignalOperation.SELL_CLOSE, tradeIntent.getPriceType(), tradeIntent.getVolume(), tradeIntent.getPrice());
			submitOrderReq(tradeIntent.getContract(), SignalOperation.SELL_OPEN, tradeIntent.getPriceType(), tradeIntent.getVolume(), tradeIntent.getPrice());
		} else {
			submitOrderReq(tradeIntent.getContract(), tradeIntent.getOperation(), tradeIntent.getPriceType(), tradeIntent.getVolume(), tradeIntent.getPrice());
		}
	}
	
	@Override
	public void cancelOrder(String originOrderId) {
		/* 回测上下文不需要撤单 */
	}
	
	@Override
	public synchronized void onOrder(Order order) {
		/* 回测上下文不接收外部的订单数据 */
	}

	@Override
	public synchronized void onTrade(Trade trade) {
		/* 回测上下文不接收外部的成交数据 */
	}
	
	@Override
	public boolean isOrderWaitTimeout(String originOrderId, long timeout) {
		if(!orderReqMap.containsKey(originOrderId)) {
			return false;
		}
		
		SubmitOrderReq orderReq = orderReqMap.get(originOrderId);
		Tick lastTick = tickMap.get(orderReq.contract());
		return lastTick.actionTimestamp() - orderReq.actionTimestamp() > timeout;
	}
	
	private Map<Contract, Tick> tickMap = new HashMap<>();
	
	@Override
	public void onTick(Tick tick) {
		pendingTrade.ifPresent(intent -> {
			if(tick.contract().equals(intent.getContract()) && tick.getTimestamp() > signalTime) {
				doTrade();
				pendingTrade = Optional.empty();
			}
		});
		synchronized (tickMap) {
			tickMap.put(tick.contract(), tick);
		}
		super.onTick(tick);
	}

	// 所有的委托都会立马转为成交单
	@Override
	public synchronized Optional<String> submitOrderReq(Contract contract, SignalOperation operation, PriceType priceType, 
			int volume, double price) {
		if(!module.isEnabled()) {
			logger.info("策略处于停用状态，忽略委托单");
			return Optional.empty();
		}
		logger.debug("回测上下文收到下单请求");
		Assert.isTrue(volume > 0, "下单手数应该为正数。当前为" + volume);
		Tick lastTick = Optional.ofNullable(tickMap.get(contract)).orElseThrow(() -> new IllegalStateException("没有行情时不应该发送订单"));
		
		double orderPrice = priceType.resolvePrice(lastTick, operation, price);
		if(logger.isInfoEnabled()) {
			logger.info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
					lastTick.actionDay(), lastTick.actionTime(),
					contract.unifiedSymbol(), operation.text(), orderPrice, volume, priceType);
		}
		String id = UUID.randomUUID().toString();
		DirectionEnum direction = OrderUtils.resolveDirection(operation);
		OffsetFlagEnum offsetFlag = operation.isOpen() ? OffsetFlagEnum.OF_Open : OffsetFlagEnum.OF_Close;
		String gatewayId = getAccount(contract).accountId();
		moduleStateMachine.onSubmitReq();
		try {
			moduleAccount.onSubmitOrder(SubmitOrderReq.builder()
							.originOrderId(id)
							.contract(contract)
							.gatewayId(gatewayId)
							.direction(direction)
							.offsetFlag(offsetFlag)
							.volume(volume)
							.price(orderPrice)
							.timeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
							.orderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
							.contingentCondition(ContingentConditionEnum.CC_Immediately)
							.actionTimestamp(lastTick.actionTimestamp())
							.minVolume(1)
							.build());
		} catch (InsufficientException e) {
			logger.error("发单失败。原因：{}", e.getMessage());
			tradeIntentMap.remove(contract);
			logger.warn("模组余额不足，主动停用模组");
			setEnabled(false);
			moduleStateMachine.onFailSubmitReq();
			return Optional.empty();
		}
		
		Order order = Order.builder()
				.gatewayId(PLAYBACK_GATEWAY)
				.contract(contract)
				.totalVolume(volume)
				.tradedVolume(volume)
				.price(lastTick.lastPrice())
				.direction(OrderUtils.resolveDirection(operation))
				.hedgeFlag(HedgeFlagEnum.HF_Speculation)
				.offsetFlag(offsetFlag)
				.tradingDay(lastTick.tradingDay())
				.updateDate(lastTick.actionDay())
				.updateTime(lastTick.actionTime())
				.orderStatus(OrderStatusEnum.OS_AllTraded)
				.build();
		moduleAccount.onOrder(order);
		moduleStateMachine.onOrder(order);
		tradeStrategy.onOrder(order);
		
		Trade trade = Trade.builder()
				.gatewayId(PLAYBACK_GATEWAY)
				.contract(contract)
				.volume(volume)
				.price(lastTick.lastPrice())
				.tradeTimestamp(lastTick.actionTimestamp())
				.direction(OrderUtils.resolveDirection(operation))
				.offsetFlag(offsetFlag)
				.priceSource(PriceSourceEnum.PSRC_LastPrice)
				.tradeDate(lastTick.actionDay())
				.tradingDay(lastTick.tradingDay())
				.tradeTime(lastTick.actionTime())
				.build();
		moduleAccount.onTrade(trade);
		moduleStateMachine.onTrade(trade);
		tradeStrategy.onTrade(trade);
		return Optional.of(id);
	}

	
	private static class MockModuleRepository implements IModuleRepository{
		
		static UnsupportedOperationException uoe() { return new UnsupportedOperationException(); }
		
		private final IModuleRepository mdRepoReal;
		
		public MockModuleRepository(IModuleRepository mdRepoReal) {
			this.mdRepoReal = mdRepoReal;
		}

		@Override
		public void saveSettings(ModuleDescription moduleSettingsDescription) { throw uoe();}

		@Override
		public ModuleDescription findSettingsByName(String moduleName) { throw uoe();}

		@Override
		public List<ModuleDescription> findAllSettings() { throw uoe();}

		@Override
		public void deleteSettingsByName(String moduleName) { throw uoe();}

		@Override
		public void saveRuntime(ModuleRuntimeDescription moduleDescription) {/* 空实现不作持久化 */}

		@Override
		public ModuleRuntimeDescription findRuntimeByName(String moduleName) { throw uoe();}

		@Override
		public void deleteRuntimeByName(String moduleName) { throw uoe();}

		@Override
		public void saveDealRecord(ModuleDealRecord dealRecord) { 
			mdRepoReal.saveDealRecord(dealRecord);
		}

		@Override
		public List<ModuleDealRecord> findAllDealRecords(String moduleName) {
			return mdRepoReal.findAllDealRecords(moduleName);
		}

		@Override
		public void removeAllDealRecords(String moduleName) { throw uoe();}
	}
}
