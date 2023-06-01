package org.dromara.northstar.gateway.sim.trade;

import org.dromara.northstar.common.utils.FieldUtils;

import cn.hutool.core.lang.Assert;
import lombok.Builder;
import lombok.NonNull;
import xyz.redtorch.pb.CoreField.TradeField;

@Builder
public class Deal {
	@NonNull
	private TradeField openTrade;
	@NonNull
	private TradeField closeTrade;
	
	public Deal(TradeField openTrade, TradeField closeTrade) {
		this.openTrade = openTrade;
		this.closeTrade = closeTrade;
		Assert.isTrue(openTrade.getContract().getContractId().equals(closeTrade.getContract().getContractId()));
		Assert.isTrue(FieldUtils.isOpen(openTrade.getOffsetFlag()));
		Assert.isTrue(FieldUtils.isClose(closeTrade.getOffsetFlag()));
	}
	
	public TradeField getCloseTrade() {
		return closeTrade;
	}

	public double profit() {
		int factor = FieldUtils.directionFactor(openTrade.getDirection());
		return factor * (closeTrade.getPrice() - openTrade.getPrice()) * closeTrade.getVolume() * closeTrade.getContract().getMultiplier();
	}
}
