package org.dromara.northstar.gateway.sim.trade;

import java.time.LocalDate;

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
		String symbol = "sim9901";
		String name = "模拟合约9901";
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
				.lastTradeDate(LocalDate.MAX)
				.build();
	}
	
	public Instrument getContract2() {
		String symbol = "sim9902";
		String name = "模拟合约9902";
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
				.lastTradeDate(LocalDate.MAX)
				.build();
	}
	
	public Instrument getContract3() {
		String symbol = "sim9903";
		String name = "模拟合约9903";
		return SimContract.builder()
				.gatewayId("SIM")
				.contractId(symbol + "@BINANCE@FUTURES@" + gatewayId)
				.currency(CurrencyEnum.USDT)
				.exchange(ExchangeEnum.BINANCE)
				.fullName(name)
				.name(name)
				.unifiedSymbol(symbol + "@BINANCE@FUTURES")
				.symbol(symbol)
				.productClass(ProductClassEnum.FUTURES)
				.thirdPartyId(symbol + "@SIM")
				.multiplier(1)
				.priceTick(1.0E-7)
				.longMarginRatio(0.2)
				.shortMarginRatio(0.2)
				.lastTradeDate(LocalDate.MAX)
				.build();
	}
}
