package org.dromara.northstar.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.model.core.Bar;
import org.dromara.northstar.common.model.core.CancelOrderReq;
import org.dromara.northstar.common.model.core.Contract;
import org.dromara.northstar.common.model.core.Order;
import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;

public class PlaybackModuleContext extends ModuleContext implements IModuleContext{
	
	public static final String PLAYBACK_GATEWAY = "回测账户";
	
	private Logger logger;
	
	private Map<Contract, Tick> lastTickMap = new HashMap<>();
	
	public PlaybackModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription, 
			ModuleAccount moduleAccount, IContractManager contractMgr, IModuleRepository moduleRepo) {
		super(tradeStrategy, moduleDescription, moduleAccount, contractMgr, new MockModuleRepository());
		logger = getLogger(getClass());
	}
	
	@Override
	public void onTick(Tick tick) {
		lastTickMap.put(tick.contract(), tick);
		super.onTick(tick);
	}

	@Override
	public void submitOrderReq(SubmitOrderReq orderReq) {
		if(!isEnabled()) {
			logger.info("策略处于停用状态，忽略委托单");
			return;
		}
		logger.debug("回测上下文收到下单请求");
		Assert.isTrue(orderReq.volume() > 0, "下单交易量应该为正数。当前为" + orderReq.volume());
		Tick lastTick = lastTickMap.get(orderReq.contract());
		if(lastTick == null) {
			throw new IllegalStateException("没有行情时不应该发送订单");
		}
		
		if(logger.isInfoEnabled()) {
			logger.info("[{} {}] 策略信号：合约【{}】，操作【{} {}】，价格【{}】，交易量【{}】", 
					lastTick.actionDay(), lastTick.actionTime(),
					orderReq.contract().unifiedSymbol(), orderReq.direction(), orderReq.offsetFlag(), orderReq.price(), orderReq.volume());
		}
		moduleStateMachine.onSubmitReq();
		Order order = Order.builder()
				.gatewayId(PLAYBACK_GATEWAY)
				.contract(orderReq.contract())
				.totalVolume(orderReq.volume())
				.tradedVolume(orderReq.volume())
				.price(lastTick.lastPrice())
				.direction(orderReq.direction())
				.offsetFlag(orderReq.offsetFlag())
				.hedgeFlag(HedgeFlagEnum.HF_Speculation)
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
				.contract(orderReq.contract())
				.volume(orderReq.volume())
				.price(lastTick.lastPrice())
				.tradeTimestamp(lastTick.actionTimestamp())
				.direction(orderReq.direction())
				.offsetFlag(orderReq.offsetFlag())
				.tradeDate(lastTick.actionDay())
				.tradingDay(lastTick.tradingDay())
				.tradeTime(lastTick.actionTime())
				.build();
		moduleAccount.onTrade(trade);
		moduleStateMachine.onTrade(trade);
		tradeStrategy.onTrade(trade);
	}
	
	
	@Override
	public void cancelOrder(CancelOrderReq cancelReq) {
		/* 回测上下文不需要撤单 */
	}
	
	@Override
	public void onOrder(Order order) {
		/* 回测上下文不接收外部的订单数据 */
	}

	@Override
	public void onBar(Bar bar) {
		try {
			super.onBar(bar);
		} catch(Exception e) {
			logger.error("", e);
		}
	}

	private static class MockModuleRepository implements IModuleRepository{
		
		static UnsupportedOperationException uoe() { return new UnsupportedOperationException(); }
		
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

	}
}
