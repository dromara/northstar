package org.dromara.northstar.module;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.exception.InsufficientException;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.support.log.ModuleLoggerFactory;
import org.dromara.northstar.support.utils.bar.BarMergerRegistry;

import cn.hutool.core.lang.Assert;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.OrderStatusEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class PlaybackModuleContext extends ModuleContext implements IModuleContext{
	
	public static final String PLAYBACK_GATEWAY = "回测账户";
	
	public PlaybackModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription,
			ModuleRuntimeDescription moduleRtDescription, IContractManager contractMgr, IModuleRepository moduleRepo,
			ModuleLoggerFactory loggerFactory, BarMergerRegistry barMergerRegistry) {
		super(tradeStrategy, moduleDescription, moduleRtDescription, contractMgr, new MockModuleRepository(moduleRepo), loggerFactory, barMergerRegistry);
	}

	@Override
	public void submitOrderReq(TradeIntent tradeIntent) {
		// 回测时直接成交，没有撤单追单逻辑
		submitOrderReq(tradeIntent.getContract(), tradeIntent.getOperation(), tradeIntent.getPriceType(), tradeIntent.getVolume(), 0);
	}
	
	@Override
	public void cancelOrder(String originOrderId) {
		/* 回测上下文不需要撤单 */
	}
	
	@Override
	public synchronized void onOrder(OrderField order) {
		/* 回测上下文不接收外部的订单数据 */
	}

	@Override
	public synchronized void onTrade(TradeField trade) {
		/* 回测上下文不接收外部的成交数据 */
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
	
	// 所有的委托都会立马转为成交单
	@Override
	public synchronized Optional<String> submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, 
			int volume, double price) {
		if(!module.isEnabled()) {
			getLogger().info("策略处于停用状态，忽略委托单");
			return Optional.empty();
		}
		getLogger().debug("回测上下文收到下单请求");
		TickField lastTick = latestTickMap.get(contract.getUnifiedSymbol());
		Assert.notNull(lastTick, "没有行情时不应该发送订单");
		Assert.isTrue(volume > 0, "下单手数应该为正数");
		
		double orderPrice = priceType.resolvePrice(lastTick, operation, price);
		if(getLogger().isInfoEnabled()) {
			getLogger().info("[{} {}] 策略信号：合约【{}】，操作【{}】，价格【{}】，手数【{}】，类型【{}】", 
					lastTick.getActionDay(), LocalTime.parse(lastTick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER),
					contract.getUnifiedSymbol(), operation.text(), orderPrice, volume, priceType);
		}
		String id = UUID.randomUUID().toString();
		DirectionEnum direction = OrderUtils.resolveDirection(operation);
		OffsetFlagEnum offsetFlag = operation.isOpen() ? OffsetFlagEnum.OF_Open : OffsetFlagEnum.OF_Close;
		String gatewayId = getAccount(contract).accountId();
		try {
			moduleAccount.onSubmitOrder(SubmitOrderReqField.newBuilder()
					.setOriginOrderId(id)
					.setContract(contract)
					.setGatewayId(gatewayId)
					.setDirection(direction)
					.setOffsetFlag(offsetFlag)
					.setVolume(volume)		
					.setPrice(orderPrice)
					.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
					.setTimeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
					.setOrderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
					.setVolumeCondition(VolumeConditionEnum.VC_AV)
					.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
					.setContingentCondition(ContingentConditionEnum.CC_Immediately)
					.setActionTimestamp(latestTickMap.get(contract.getUnifiedSymbol()).getActionTimestamp())
					.setMinVolume(1)
					.build());
		} catch (InsufficientException e) {
			getLogger().error("发单失败。原因：{}", e.getMessage());
			tradeIntentMap.remove(contract.getUnifiedSymbol());
			getLogger().warn("模组余额不足，主动停用模组");
			setEnabled(false);
			return Optional.empty();
		}
		
		OrderField order = OrderField.newBuilder()
				.setGatewayId(PLAYBACK_GATEWAY)
				.setAccountId(PLAYBACK_GATEWAY)
				.setContract(contract)
				.setTotalVolume(volume)
				.setTradedVolume(volume)
				.setPrice(lastTick.getLastPrice())
				.setDirection(OrderUtils.resolveDirection(operation))
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setOffsetFlag(offsetFlag)
				.setTradingDay(lastTick.getTradingDay())
				.setOrderDate(lastTick.getActionDay())
				.setOrderTime(lastTick.getActionTime())
				.setOrderStatus(OrderStatusEnum.OS_AllTraded)
				.build();
		moduleAccount.onOrder(order);
		tradeStrategy.onOrder(order);
		
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
				.setOffsetFlag(offsetFlag)
				.setPriceSource(PriceSourceEnum.PSRC_LastPrice)
				.setTradeDate(lastTick.getActionDay())
				.setTradingDay(lastTick.getTradingDay())
				.setTradeTime(LocalTime.parse(lastTick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER).format(DateTimeConstant.T_FORMAT_FORMATTER))
				.build();
		moduleAccount.onTrade(trade);
		tradeStrategy.onTrade(trade);
		return Optional.of(id);
	}

	
	private static class MockModuleRepository implements IModuleRepository{
		
		static UnsupportedOperationException uoe() { return new UnsupportedOperationException(); }
		
		private IModuleRepository mdRepoReal;
		
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
