package org.dromara.northstar.gateway.sim.trade;

import org.dromara.northstar.common.model.core.SubmitOrderReq;
import org.dromara.northstar.common.model.core.Tick;
import org.dromara.northstar.common.model.core.Trade;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import xyz.redtorch.pb.CoreEnum.OrderPriceTypeEnum;
import xyz.redtorch.pb.CoreEnum.PriceSourceEnum;

/**
 * 成交
 * @author KevinHuangwl
 *
 */
@Builder
@Data
@AllArgsConstructor
public class Transaction {
	
	private Tick dealTick;
	
	private SubmitOrderReq orderReq;
	
	public Trade trade() {
		return Trade.builder()
				.contract(orderReq.contract())
				.tradeTimestamp(dealTick.actionTimestamp())
				.direction(orderReq.direction())
				.gatewayId(orderReq.gatewayId())
				.hedgeFlag(orderReq.hedgeFlag())
				.offsetFlag(orderReq.offsetFlag())
				.orderId(orderReq.originOrderId())
				.originOrderId(orderReq.originOrderId())
				.price(dealPrice())
				.priceSource(PriceSourceEnum.PSRC_LastPrice)
				.tradeDate(dealTick.actionDay())
				.tradingDay(dealTick.tradingDay())
				.tradeTime(dealTick.actionTime())
				.volume(orderReq.volume())
				.build();
	}
	
	public double dealPrice() {
		double dealPrice = 0;
		if(orderReq.orderPriceType() == OrderPriceTypeEnum.OPT_AnyPrice) {
			dealPrice = switch(orderReq.direction()) {
				case D_Buy -> dealTick.askPrice().get(0) > 0 ? dealTick.askPrice().get(0) : dealTick.lastPrice();
				case D_Sell -> dealTick.bidPrice().get(0) > 0 ? dealTick.bidPrice().get(0) : dealTick.lastPrice();
				default -> throw new IllegalArgumentException("Unexpected value: " + orderReq.direction());
			};
		} else {
			dealPrice = dealTick.lastPrice();
		}
		return dealPrice;
	}
}
