package org.dromara.northstar.gateway.sim.trade;

import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;
import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class Deal {
	@NonNull
	private Trade openTrade;
	@NonNull
	private Trade closeTrade;
	
	public Deal(Trade openTrade, Trade closeTrade) {
		this.openTrade = openTrade;
		this.closeTrade = closeTrade;
		Assert.isTrue(openTrade.contract().equals(closeTrade.contract()), 
				() -> "开平仓合约不一致。" + String.format("[%s] => [%s]", openTrade.contract().name(), closeTrade.contract().name()));
		Assert.isTrue(FieldUtils.isOpen(openTrade.offsetFlag()), "第一个参数应该为开仓成交");
		Assert.isTrue(FieldUtils.isClose(closeTrade.offsetFlag()), "第二个参数应该为平仓成交");
	}
	
	public double profit() {
		int factor = FieldUtils.directionFactor(openTrade.direction());
		return factor * (closeTrade.price() - openTrade.price()) * closeTrade.volume() * Math.pow(10, -1 * closeTrade.contract().quantityPrecision())
				* closeTrade.contract().multiplier();
	}
}
