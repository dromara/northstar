package tech.xuanwu.northstar.strategy.api.utils;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.api.constant.PriceType;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class PriceResolver {
	
	private PriceResolver() {}
	
	public static double getPrice(PriceType type, double signalPrice, TickField tick, boolean isBuy, double overprice) {
		int factor = isBuy ? 1 : -1;
		double orderPrice;
		switch(type) {
		case OPP_PRICE:
			double oppPrice = isBuy ? tick.getAskPrice(0) : tick.getBidPrice(0);
			orderPrice = oppPrice + factor * overprice;
			log.info("当前使用[对手价]成交，基础价为：{}，超价：{} Tick，最终下单价：{}", oppPrice, overprice, orderPrice);
			break;
		case ANY_PRICE:
			orderPrice = isBuy ? tick.getUpperLimit() : tick.getLowerLimit();
			log.info("当前使用[市价]成交，最终下单价：{}", orderPrice);
			break;
		case LAST_PRICE:
			orderPrice = tick.getLastPrice() + factor *  overprice;
			log.info("当前使用[最新价]成交，基础价为：{}，超价：{} Tick，最终下单价：{}", tick.getLastPrice(), overprice, orderPrice);
			break;
		case WAITING_PRICE:
			orderPrice = isBuy ? tick.getBidPrice(0) : tick.getAskPrice(0);
			log.info("当前使用[排队价]成交，基础价为：{}，忽略超价，最终下单价：{}", orderPrice, orderPrice);
			break;
		case SIGNAL_PRICE:
			orderPrice = signalPrice + factor *  overprice;
			log.info("当前使用[限价]成交，基础价为：{}，超价：{} Tick，最终下单价：{}", signalPrice, overprice, orderPrice);
			break;
		default:
			throw new IllegalStateException("未知下单价格类型：" + type);
		}
		return orderPrice;
	}

}
