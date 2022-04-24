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
import tech.quantit.northstar.strategy.api.IMarketDataStore;
import tech.quantit.northstar.strategy.api.IModule;
import tech.quantit.northstar.strategy.api.IModuleAccountStore;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.IModuleOrderingStore;
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
	
	protected IMarketDataStore mktStore;
	
	protected IModuleAccountStore accStore;
	
	protected IModuleOrderingStore orderStore;
	
	protected IModule module;
	
	protected Map<String, Boolean> orderIdMap = new HashMap<>();
	
	private Map<String, TradeGateway> gatewayMap = new HashMap<>();
	
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
					.logicalPosition(accStore.getLogicalPosition(gatewayId))
					.logicalPositionProfit(accStore.getLogicalPositionProfit(gatewayId))
					.uncloseTrades(accStore.getUncloseTrade(gatewayId).stream().map(TradeField::toByteArray).toList())
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
				.moduleState(orderStore.getModuleState())
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

	@Override
	public void onTick(TickField tick) {
		if(!StringUtils.equals(tradingDay, tick.getTradingDay())) {
			tradingDay = tick.getTradingDay();
		}
		tradeStrategy.onTick(tick);
	}
	
	@Override
	public void onBar(BarField bar) {
		tradeStrategy.onBar(bar);
	}
	
	@Override
	public void onOrder(OrderField order) {
		if(OrderUtils.isValidOrder(order)) {
			orderIdMap.put(order.getOriginOrderId(), Boolean.TRUE);
		} else {
			orderIdMap.remove(order.getOriginOrderId());
		}
		tradeStrategy.onOrder(order);
	}

	@Override
	public void onTrade(TradeField trade) {
		if(orderIdMap.containsKey(trade.getOriginOrderId())) {
			orderIdMap.remove(trade.getOriginOrderId());
		}
		tradeStrategy.onTrade(trade);
	}

	@Override
	public void setComponent(ContextAware component) {
		component.setContext(this);
		if(component instanceof IModuleAccountStore store) {
			this.accStore = store;
		}
		if (component instanceof IModuleOrderingStore store) {
			this.orderStore = store;
		}
		if (component instanceof IMarketDataStore store) {
			this.mktStore = store;
		}
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
