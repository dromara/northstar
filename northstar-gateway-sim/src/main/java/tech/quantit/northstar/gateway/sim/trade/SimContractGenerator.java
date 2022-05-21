package tech.quantit.northstar.gateway.sim.trade;

import tech.quantit.northstar.common.constant.GatewayType;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

public class SimContractGenerator {
	
	private String gatewayId;
	
	public SimContractGenerator(String gatewayId) {
		this.gatewayId = gatewayId;
	}
	
	public ContractField getContract() {
		String symbol = "sim9999";
		String name = "模拟品种9999";
		return ContractField.newBuilder()
				.setGatewayId(GatewayType.SIM.toString())
				.setContractId(symbol + "@SHFE@FUTURES@" + gatewayId)
				.setCurrency(CurrencyEnum.CNY)
				.setExchange(ExchangeEnum.SHFE)
				.setFullName(name)
				.setName(name)
				.setUnifiedSymbol(symbol + "@SHFE@FUTURES")
				.setSymbol(symbol)
				.setProductClass(ProductClassEnum.FUTURES)
				.setThirdPartyId(symbol + "@" + GatewayType.SIM)
				.setMultiplier(10)
				.setPriceTick(1)
				.setLongMarginRatio(0.08)
				.setShortMarginRatio(0.08)
				.build();
	}
	
	public ContractField getContract2() {
		String symbol = "sim999";
		String name = "模拟品种999";
		return ContractField.newBuilder()
				.setGatewayId(GatewayType.SIM.toString())
				.setContractId(symbol + "@CZCE@FUTURES@" + gatewayId)
				.setCurrency(CurrencyEnum.CNY)
				.setExchange(ExchangeEnum.CZCE)
				.setFullName(name)
				.setName(name)
				.setUnifiedSymbol(symbol + "@CZCE@FUTURES")
				.setSymbol(symbol)
				.setProductClass(ProductClassEnum.FUTURES)
				.setThirdPartyId(symbol + "@" + GatewayType.SIM)
				.setMultiplier(10)
				.setPriceTick(0.5)
				.setLongMarginRatio(0.08)
				.setShortMarginRatio(0.08)
				.build();
	}
}
