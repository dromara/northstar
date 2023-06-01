package org.dromara.northstar.gateway.sim.trade;

import java.time.LocalTime;

import org.dromara.northstar.common.constant.DateTimeConstant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;
import xyz.redtorch.pb.CoreField.TradeField;

/**
 * 成交
 * @author KevinHuangwl
 *
 */
@Builder
@Data
@AllArgsConstructor
public class Transaction {
	
	private TickField dealTick;
	
	private SubmitOrderReqField orderReq;
	
	public TradeField tradeField() {
		return TradeField.newBuilder()
				.setTradeId(System.currentTimeMillis()+"")
				.setAccountId(orderReq.getGatewayId())
				.setAdapterOrderId("")
				.setContract(orderReq.getContract())
				.setTradeTimestamp(dealTick.getActionTimestamp())
				.setDirection(orderReq.getDirection())
				.setGatewayId(orderReq.getGatewayId())
				.setHedgeFlag(orderReq.getHedgeFlag())
				.setOffsetFlag(orderReq.getOffsetFlag())
				.setOrderId(orderReq.getOriginOrderId())
				.setOriginOrderId(orderReq.getOriginOrderId())
				.setPrice(dealPrice())
				.setPriceSource(PriceSourceEnum.PSRC_LastPrice)
				.setTradeDate(dealTick.getActionDay())
				.setTradingDay(dealTick.getTradingDay())
				.setTradeTime(LocalTime.parse(dealTick.getActionTime(), DateTimeConstant.T_FORMAT_WITH_MS_INT_FORMATTER).format(DateTimeConstant.T_FORMAT_FORMATTER))
				.setVolume(orderReq.getVolume())
				.build();
	}
	
	public double dealPrice() {
		double dealPrice = 0;
		if(orderReq.getOrderPriceType() == OrderPriceTypeEnum.OPT_AnyPrice) {
			dealPrice = switch(orderReq.getDirection()) {
				case D_Buy -> dealTick.getAskPrice(0) > 0 ? dealTick.getAskPrice(0) : dealTick.getLastPrice();
				case D_Sell -> dealTick.getBidPrice(0) > 0 ? dealTick.getBidPrice(0) : dealTick.getLastPrice();
				default -> throw new IllegalArgumentException("Unexpected value: " + orderReq.getDirection());
			};
		} else {
			dealPrice = orderReq.getPrice() > 0 ? orderReq.getPrice() : dealTick.getLastPrice();
		}
		return dealPrice;
	}
}
