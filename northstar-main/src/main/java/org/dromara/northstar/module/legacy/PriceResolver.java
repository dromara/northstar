package org.dromara.northstar.module.legacy;

import org.dromara.northstar.strategy.constant.PriceType;

import lombok.extern.slf4j.Slf4j;
import xyz.redtorch.pb.CoreField.TickField;

@Deprecated
@Slf4j
public class PriceResolver {
	
	private PriceResolver() {}
	
	public static double getPrice(PriceType type, double signalPrice, TickField tick, boolean isBuy) {
		double orderPrice;
		switch(type) {
		case OPP_PRICE:
			orderPrice = isBuy ? tick.getAskPrice(0) : tick.getBidPrice(0);
			break;
		case ANY_PRICE:
			orderPrice = 0;
			break;
		case LAST_PRICE:
			orderPrice = tick.getLastPrice();
			break;
		case WAITING_PRICE:
			orderPrice = isBuy ? tick.getBidPrice(0) : tick.getAskPrice(0);
			break;
		default:
			throw new IllegalStateException("未知下单价格类型：" + type);
		}
		log.info("当前使用[{}]成交，下单价：{}", type.getName(), orderPrice);
		return orderPrice;
	}

}
