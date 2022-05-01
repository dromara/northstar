package tech.quantit.northstar.domain.module;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModulePositionDescription;
import tech.quantit.northstar.common.utils.OrderUtils;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import tech.quantit.northstar.strategy.api.ContextAware;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
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
public class ModuleContext implements IModuleContext{
	
	protected TradeStrategy tradeStrategy;
	
	protected IModuleAccountStore accStore;
	
	protected IModule module;
	
	/* originOrderId -> boolean */
	protected Map<String, Boolean> orderIdMap = new HashMap<>();
	
	/* gatewayId -> gateway */
	private Map<String, TradeGateway> gatewayMap = new HashMap<>();
	
	/* unifiedSymbol -> barMerger */
	private Map<String, BarMerger> contractBarMergerMap = new HashMap<>();
	
	private String tradingDay;
	
	private ClosingStrategy closingStrategy;
	
	public ModuleContext(ClosingStrategy closingStrategy, TradeGateway...gateways) {
		this.closingStrategy = closingStrategy;
		for(TradeGateway tradeGateway : gateways) {
			gatewayMap.put(tradeGateway.getGatewaySetting().getGatewayId(), tradeGateway);
		}
	}

	@Override
	public ModuleDescription getModuleDescription() {
		Map<String, ModuleAccountDescription> accMap = new HashMap<>();
		for(String gatewayId : gatewayMap.keySet()) {
			ModulePositionDescription posDescription = ModulePositionDescription.builder()
					.logicalPositions(accStore.getPositions(gatewayId).stream().map(PositionField::toByteArray).toList())
					.uncloseTrades(accStore.getUncloseTrades(gatewayId).stream().map(TradeField::toByteArray).toList())
					.build();
			
			ModuleAccountDescription accDescription = ModuleAccountDescription.builder()
					.initBalance(accStore.getInitBalance(gatewayId))
					.preBalance(accStore.getPreBalance(gatewayId))
					.accCloseProfit(accStore.getAccCloseProfit(gatewayId))
					.accDealVolume(accStore.getAccDealVolume(gatewayId))
					.positionDescription(posDescription)
					.build();
			accMap.put(gatewayId, accDescription);
		}
		return ModuleDescription.builder()
				.moduleName(module.getName())
				.enabled(module.isEnabled())
				.moduleState(accStore.getModuleStateMachine().getState())
				.accountDescriptions(accMap)
				.build();
	}

	@Override
	public String submitOrderReq(String gatewayId, ContractField contract, SignalOperation operation,
			PriceType priceType, int volume, double price) {
		String id = UUID.randomUUID().toString();
		submitOrderReq(SubmitOrderReqField.newBuilder()
				.setOriginOrderId(id)
				.setContract(contract)
				.setGatewayId(gatewayId)
				.setDirection(OrderUtils.resolveDirection(operation))
				.setOffsetFlag(closingStrategy.resolveOperation(operation, accStore))
				.setPrice(price)
				.setVolume(volume)		//	当信号交易量大于零时，优先使用信号交易量
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(priceType == PriceType.ANY_PRICE ? TimeConditionEnum.TC_IOC : TimeConditionEnum.TC_GFD)
				.setOrderPriceType(priceType == PriceType.ANY_PRICE ? OrderPriceTypeEnum.OPT_AnyPrice : OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.build());
		return id;
	}

	@Override
	public String submitOrderReq(SubmitOrderReqField orderReq) {
		String gatewayId = orderReq.getGatewayId();
		if(!gatewayMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关：" + gatewayId);
		}
		gatewayMap.get(gatewayId).submitOrder(orderReq);
		orderIdMap.put(orderReq.getOriginOrderId(), Boolean.FALSE);
		return orderReq.getOriginOrderId();
	}
	
	@Override
	public void cancelOrderReq(CancelOrderReqField cancelReq) {
		String gatewayId = cancelReq.getGatewayId();
		if(!gatewayMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("找不到网关：" + gatewayId);
		}
		gatewayMap.get(gatewayId).cancelOrder(cancelReq);
	}

	/* 此处收到的TICK数据是所有订阅的数据，需要过滤 */
	@Override
	public void onTick(TickField tick) {
		if(!contractBarMergerMap.containsKey(tick.getUnifiedSymbol())) {
			return;
		}
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
		}
		tradeStrategy.onTick(tick, module.isEnabled());
	}
	
	/* 此处收到的BAR数据是所有订阅的数据，需要过滤 */
	@Override
	public void onBar(BarField bar) {
		if(!contractBarMergerMap.containsKey(bar.getUnifiedSymbol())) {
			return;
		}
		tradeStrategy.onBar(bar, module.isEnabled());
	}
	
	/* 此处收到的ORDER数据是所有订单回报，需要过滤 */
	@Override
	public void onOrder(OrderField order) {
		if(OrderUtils.isValidOrder(order)) {
			orderIdMap.put(order.getOriginOrderId(), Boolean.TRUE);
		} else {
			orderIdMap.remove(order.getOriginOrderId());
		}
		accStore.onOrder(order);
		tradeStrategy.onOrder(order);
	}

	/* 此处收到的TRADE数据是所有成交回报，需要过滤 */
	@Override
	public void onTrade(TradeField trade) {
		if(orderIdMap.containsKey(trade.getOriginOrderId())) {
			orderIdMap.remove(trade.getOriginOrderId());
		}
		accStore.onTrade(trade);
		tradeStrategy.onTrade(trade);
	}

	@Override
	public void setAccountStore(IModuleAccountStore store) {
		store.setContext(this);
		this.accStore = store;
	}

	@Override
	public void setTradeStrategy(TradeStrategy strategy) {
		tradeStrategy = strategy;
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
	public void setModule(IModule module) {
		this.module = module;
	}

	@Override
	public String getModuleName() {
		return module.getName();
	}

	@Override
	public ClosingPolicy getClosingPolicy() {
		return closingStrategy.getClosingPolicy();
	}

}
