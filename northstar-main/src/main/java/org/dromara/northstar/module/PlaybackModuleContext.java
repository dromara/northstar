package org.dromara.northstar.module;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dromara.northstar.common.constant.DateTimeConstant;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.common.model.ModuleDealRecord;
import org.dromara.northstar.common.model.ModuleDescription;
import org.dromara.northstar.common.model.ModuleRuntimeDescription;
import org.dromara.northstar.common.utils.FieldUtils;
import org.dromara.northstar.common.utils.OrderUtils;
import org.dromara.northstar.data.IModuleRepository;
import org.dromara.northstar.gateway.IContractManager;
import org.dromara.northstar.strategy.IMessageSender;
import org.dromara.northstar.strategy.IModuleContext;
import org.dromara.northstar.strategy.TradeStrategy;
import org.dromara.northstar.strategy.constant.PriceType;
import org.dromara.northstar.strategy.model.TradeIntent;
import org.dromara.northstar.support.log.ModuleLoggerFactory;
import org.dromara.northstar.support.notification.IMessageSenderManager;

import cn.hutool.core.lang.Assert;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class PlaybackModuleContext extends ModuleContext implements IModuleContext{
	
	public static final String PLAYBACK_GATEWAY = "回测账户";
	
	private static final IMessageSender sender = new IMessageSender() {
		
		@Override
		public void send(String receiver, String content) {/* 占位不实现 */}
		
		@Override
		public void send(String receiver, String title, String content) {/* 占位不实现 */}
	};
	
	private static final IMessageSenderManager mockSenderMgr = new IMessageSenderManager() {

		@Override
		public IMessageSender getSender() {
			return sender;
		}

		@Override
		public List<String> getSubscribers() {
			return Collections.emptyList();
		}
		
	};
	
	public PlaybackModuleContext(TradeStrategy tradeStrategy, ModuleDescription moduleDescription,
			ModuleRuntimeDescription moduleRtDescription, IContractManager contractMgr, IModuleRepository moduleRepo,
			ModuleLoggerFactory loggerFactory) {
		super(tradeStrategy, moduleDescription, moduleRtDescription, contractMgr, new MockModuleRepository(moduleRepo), loggerFactory, mockSenderMgr);
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
	
	// 所有的委托都会立马转为成交单
	@Override
	public synchronized Optional<String> submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume,
			double price) {
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
		List<TradeField> nonclosedTrades = moduleAccount.getNonclosedTrades(contract.getUnifiedSymbol(), FieldUtils.getOpposite(direction));
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
				.setOffsetFlag(module.getModuleDescription().getClosingPolicy().resolveOffsetFlag(operation, contract, nonclosedTrades, lastTick.getTradingDay()))
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
		public List<ModuleDealRecord> findAllDealRecords(String moduleName) { throw uoe();}

		@Override
		public void removeAllDealRecords(String moduleName) { throw uoe();}
	}
}
