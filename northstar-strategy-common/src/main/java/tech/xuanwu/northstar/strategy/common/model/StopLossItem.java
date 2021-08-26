package tech.xuanwu.northstar.strategy.common.model;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.OrderField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class StopLossItem {

	private TradeField trade;
	
	private boolean isBuy;
	private boolean isSell;
	
	private double stopPrice;
	
	private StopLossItem(TradeField trade, double stopPrice) {
		this.stopPrice = stopPrice;
		this.trade = trade;
		isBuy = trade.getDirection() == DirectionEnum.D_Buy;
		isSell = trade.getDirection() == DirectionEnum.D_Sell;
	}
	
	public static Optional<StopLossItem> generateFrom(TradeField trade, OrderField order){
		if(trade.getDirection() == DirectionEnum.D_Unknown) {
			throw new IllegalStateException("未知的交易方向：" + trade.toString())  ;
		}
		if(trade.getOffsetFlag() == OffsetFlagEnum.OF_Open && order.getStopPrice() > 0) {
			return Optional.of(new StopLossItem(trade, order.getStopPrice()));
		}
		return Optional.empty();
	}
	
	public Optional<SubmitOrderReqField> onTick(TickField tick){
		if(!StringUtils.equals(tick.getGatewayId(), trade.getGatewayId()) 
				|| !StringUtils.equals(tick.getUnifiedSymbol(), trade.getContract().getUnifiedSymbol())) {
			return Optional.empty();
		}
		if(isBuy && tick.getLastPrice() <= stopPrice || isSell && tick.getLastPrice() >= stopPrice) {
			// 生成止损
			return Optional.of(SubmitOrderReqField.newBuilder()
					.setOriginOrderId(UUID.randomUUID().toString())
					.setContract(trade.getContract())
					.setDirection(isBuy ? DirectionEnum.D_Sell : DirectionEnum.D_Buy)
					.setVolume(trade.getVolume())
					.setPrice(0) 											//市价专用
					.setOrderPriceType(OrderPriceTypeEnum.OPT_AnyPrice)	//市价专用
					.setTimeCondition(TimeConditionEnum.TC_IOC)				//市价专用
					.setOffsetFlag(StringUtils.equals(tick.getTradingDay(), trade.getTradingDay()) ? OffsetFlagEnum.OF_CloseToday : OffsetFlagEnum.OF_CloseYesterday)
					.setVolumeCondition(VolumeConditionEnum.VC_AV)
					.setContingentCondition(ContingentConditionEnum.CC_Immediately)
					.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
					.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
					.build());
		}
		
		return Optional.empty();
	}
}
