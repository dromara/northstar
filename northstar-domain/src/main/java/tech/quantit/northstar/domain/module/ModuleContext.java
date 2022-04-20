package tech.quantit.northstar.domain.module;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import tech.quantit.northstar.common.Subscribable;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.exception.NoSuchElementException;
import tech.quantit.northstar.common.model.ModuleAccountDescription;
import tech.quantit.northstar.common.model.ModuleDescription;
import tech.quantit.northstar.common.model.ModulePositionDescription;
import tech.quantit.northstar.common.utils.OrderUtils;
import tech.quantit.northstar.gateway.api.TradeGateway;
import tech.quantit.northstar.strategy.api.IModuleContext;
import tech.quantit.northstar.strategy.api.TradeStrategy;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
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

public class ModuleContext implements IModuleContext{
	
	private TradeStrategy bindedStrategy;
	
	private IModuleAccountStore accStore;
	
	private IModuleOrderingStore orderStore;
	
	private Map<String, TradeGateway> gatewayMap = new HashMap<>();
	
	private Map<String, Boolean> orderIdMap = new HashMap<>();
	
	private TickField lastTick;
	
	@Override
	public void setTradeStrategy(TradeStrategy strategy) {
		bindedStrategy = strategy;
	}

	@Override
	public ModuleDescription getModuleDescription() {
		ModuleAccountDescription accDescription = ModuleAccountDescription.builder()
				.initBalance(accStore.getInitBalance())
				.preBalance(accStore.getPreBalance())
				.build();
		
		ModulePositionDescription posDescription = ModulePositionDescription.builder()
				.logicalPosition(accStore.getLogicalPosition())
				.logicalPositionProfit(accStore.getLogicalPositionProfit())
				.uncloseTrades(accStore.getUncloseTrade().stream().map(TradeField::toByteArray).toList())
				.build();
		
		return ModuleDescription.builder()
				.moduleState(orderStore.getModuleState())
				.accountDescription(accDescription)
				.positionDescription(posDescription)
				.build();
	}

	@Override
	public String submitOrderReq(String gatewayId, ContractField contract, SignalOperation operation,
			PriceType priceType, int volume, double price) {
		String id = UUID.randomUUID().toString();
		OffsetFlagEnum offsetFlag = OffsetFlagEnum.OF_Unknown;
		if(operation.isClose()) {
			DirectionEnum dir = operation.isBuy() ? DirectionEnum.D_Sell : DirectionEnum.D_Buy;
			Optional<TradeField> opt = accStore.getUncloseTrade(contract.getUnifiedSymbol(), dir);
			if(opt.isPresent()) {
				offsetFlag = StringUtils.equals(opt.get().getTradingDay(), lastTick.getTradingDay()) 
						? OffsetFlagEnum.OF_CloseToday 
						: OffsetFlagEnum.OF_Close;
			}
		}
		if(operation.isOpen()) {
			offsetFlag = OffsetFlagEnum.OF_Open;
		}
		submitOrderReq(SubmitOrderReqField.newBuilder()
				.setOriginOrderId(id)
				.setContract(contract)
				.setGatewayId(gatewayId)
				.setDirection(OrderUtils.resolveDirection(operation))
				.setOffsetFlag(offsetFlag)
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
			throw new NoSuchElementException("模组没有绑定此网关：" + gatewayId);
		}
		gatewayMap.get(gatewayId).submitOrder(orderReq);
		orderIdMap.put(orderReq.getOriginOrderId(), Boolean.FALSE);
		return orderReq.getOriginOrderId();
	}

	@Override
	public void cancelOrderReq(CancelOrderReqField cancelReq) {
		String gatewayId = cancelReq.getGatewayId();
		if(!gatewayMap.containsKey(gatewayId)) {
			throw new NoSuchElementException("模组没有绑定此网关：" + gatewayId);
		}
		gatewayMap.get(gatewayId).cancelOrder(cancelReq);
	}

	@Override
	public void onTick(TickField tick) {
		lastTick = tick;
		bindedStrategy.onTick(tick);
	}

	@Override
	public void onBar(BarField bar) {
		bindedStrategy.onBar(bar);
	}

	@Override
	public void onOrder(OrderField order) {
		if(OrderUtils.isValidOrder(order)) {
			orderIdMap.put(order.getOriginOrderId(), Boolean.TRUE);
		}
		bindedStrategy.onOrder(order);
	}

	@Override
	public void onTrade(TradeField trade) {
		if(orderIdMap.containsKey(trade.getOriginOrderId())) {
			orderIdMap.remove(trade.getOriginOrderId());
		}
		bindedStrategy.onTrade(trade);
	}

	@Override
	public void setComponent(Subscribable component) {
		if(component instanceof IModuleAccountStore store) {
			this.accStore = store;
		}
		if (component instanceof IModuleOrderingStore store) {
			this.orderStore = store;
		}
	}

}
