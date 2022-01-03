package test.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import tech.quantit.northstar.common.constant.DateTimeConstant;
import xyz.redtorch.pb.CoreEnum.ContingentConditionEnum;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ForceCloseReasonEnum;
import xyz.redtorch.pb.CoreEnum.HedgeFlagEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreEnum.TimeConditionEnum;
import xyz.redtorch.pb.CoreEnum.VolumeConditionEnum;
import xyz.redtorch.pb.CoreField.CancelOrderReqField;
import xyz.redtorch.pb.CoreField.ContractField;
import xyz.redtorch.pb.CoreField.GatewaySettingField;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

public class TestFieldFactory {
	
	private String gatewayId;
	
	public TestFieldFactory(String gatewayId) {
		this.gatewayId = gatewayId;
	}
	
	public SubmitOrderReqField makeOrderReq(String symbol, DirectionEnum direction, OffsetFlagEnum offsetFlag, int openVol, double price, double stopPrice) {
		return SubmitOrderReqField.newBuilder()
				.setOriginOrderId(UUID.randomUUID().toString())
				.setContract(makeContract(symbol))
				.setDirection(direction)
				.setOffsetFlag(offsetFlag)
				.setOrderPriceType(OrderPriceTypeEnum.OPT_LimitPrice)
				.setVolume(openVol)
				.setHedgeFlag(HedgeFlagEnum.HF_Speculation)
				.setTimeCondition(TimeConditionEnum.TC_GFD)
				.setVolumeCondition(VolumeConditionEnum.VC_AV)
				.setForceCloseReason(ForceCloseReasonEnum.FCR_NotForceClose)
				.setContingentCondition(ContingentConditionEnum.CC_Immediately)
				.setMinVolume(1)
				.setGatewayId(gatewayId)
				.setStopPrice(stopPrice)
				.setPrice(price)
				.build();
	}
	
	public CancelOrderReqField makeCancelReq(SubmitOrderReqField orderReq) {
		return CancelOrderReqField.newBuilder()
				.setGatewayId(orderReq.getGatewayId())
				.setOriginOrderId(orderReq.getOriginOrderId())
				.build();
	}
	
	public ContractField makeContract(String symbol) {
		return ContractField.newBuilder()
				.setGatewayId(gatewayId)
				.setCurrency(CurrencyEnum.CNY)
				.setContractId(symbol + "@SHFE@FUTURES@" + gatewayId)
				.setExchange(ExchangeEnum.SHFE)
				.setFullName(symbol)
				.setLongMarginRatio(0.08)
				.setShortMarginRatio(0.08)
				.setMultiplier(10)
				.setPriceTick(1)
				.setProductClass(ProductClassEnum.FUTURES)
				.setUnifiedSymbol(symbol + "@SHFE@FUTURES")
				.setSymbol(symbol)
				.build();
	}
	
	public TickField makeTickField(String symbol, double price) {
		return TickField.newBuilder()
				.setGatewayId(gatewayId)
				.setUnifiedSymbol(symbol + "@SHFE@FUTURES")
				.setActionDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.setActionTime(LocalTime.now().format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setActionTimestamp(System.currentTimeMillis())
				.addAllAskPrice(List.of(price + 1D, 0D, 0D, 0D, 0D))
				.addAllBidPrice(List.of(price - 1D, 0D, 0D, 0D, 0D))
				.setLastPrice(price)
				.setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.build();
	}
	
	public GatewaySettingField makeGatewaySetting() {
		return GatewaySettingField.newBuilder()
				.setGatewayId(gatewayId)
				.build();
	}
	
	public TradeField makeTradeField(String symbol, double price, int vol, DirectionEnum dir, OffsetFlagEnum offset) {
		return TradeField.newBuilder()
				.setOriginOrderId(UUID.randomUUID().toString())
				.setContract(makeContract(symbol))
				.setPrice(price)
				.setVolume(vol)
				.setDirection(dir)
				.setOffsetFlag(offset)
				.setTradingDay(LocalDate.now().format(DateTimeConstant.D_FORMAT_INT_FORMATTER))
				.build();
	}

}