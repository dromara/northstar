package tech.quantit.northstar.gateway.sim.trade;

import java.time.LocalDate;

import tech.quantit.northstar.common.constant.Constants;
import xyz.redtorch.pb.CoreEnum.CurrencyEnum;
import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;
import xyz.redtorch.pb.CoreField.ContractField;

public class SimContractGenerator {
	
	public ContractField getContract(String gatewayId) {
		LocalDate date = LocalDate.now().plusDays(45);
		String year = date.getYear() % 100 + "";
		String month = String.format("%02d", date.getMonth().getValue());
		String symbol = "sim" + year + month;
		String name = "模拟品种" + year + month;
		return ContractField.newBuilder()
				.setGatewayId(Constants.SIM_MKT_GATEWAY_ID)
				.setContractId(symbol + "@SHFE@FUTURES@" + gatewayId)
				.setCurrency(CurrencyEnum.CNY)
				.setExchange(ExchangeEnum.SHFE)
				.setFullName(name)
				.setName(name)
				.setUnifiedSymbol(symbol + "@SHFE@FUTURES")
				.setSymbol(symbol)
				.setProductClass(ProductClassEnum.FUTURES)
				.setThirdPartyId(symbol)
				.setMultiplier(1)
				.setPriceTick(10)
				.setLongMarginRatio(0.08)
				.setShortMarginRatio(0.08)
				.build();
	}
}
