package org.dromara.northstar.gateway.sim.trade;

import org.dromara.northstar.gateway.Instrument;

import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

public class SimContractGenerator {
	
	private String gatewayId;
	
	public SimContractGenerator(String gatewayId) {
		this.gatewayId = gatewayId;
	}
	
	public Instrument getContract() {
		String symbol = "sim9999";
		String name = "模拟合约";
		return SimContract.builder()
				.gatewayId("SIM")
				.contractId(symbol + "@SHFE@FUTURES@" + gatewayId)
				.currency(CurrencyEnum.CNY)
				.exchange(ExchangeEnum.SHFE)
				.fullName(name)
				.name(name)
				.unifiedSymbol(symbol + "@SHFE@FUTURES")
				.symbol(symbol)
				.productClass(ProductClassEnum.FUTURES)
				.thirdPartyId(symbol + "@SIM")
				.multiplier(10)
				.priceTick(1)
				.longMarginRatio(0.08)
				.shortMarginRatio(0.08)
				.lastTradeDateOrContractMonth(name)
				.build();
	}
	
}
