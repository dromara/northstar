package org.dromara.northstar.gateway.sim.trade;

import org.dromara.northstar.common.model.core.Trade;
import org.dromara.northstar.common.utils.FieldUtils;

import cn.hutool.core.lang.Assert;
import lombok.Builder;
import lombok.NonNull;

@Builder
public class Deal {
	@NonNull
	private Trade openTrade;
	@NonNull
	private Trade closeTrade;
	
	public Deal(Trade openTrade, Trade closeTrade) {
		this.openTrade = openTrade;
		this.closeTrade = closeTrade;
		Assert.isTrue(openTrade.contract().equals(closeTrade.contract()));
		Assert.isTrue(FieldUtils.isOpen(openTrade.offsetFlag()));
		Assert.isTrue(FieldUtils.isClose(closeTrade.offsetFlag()));
	}
	
	public Trade getCloseTrade() {
		return closeTrade;
	}

	public double profit() {
		int factor = FieldUtils.directionFactor(openTrade.direction());
		return factor * (closeTrade.price() - openTrade.price()) * closeTrade.volume() * closeTrade.contract().multiplier();
	}
}
