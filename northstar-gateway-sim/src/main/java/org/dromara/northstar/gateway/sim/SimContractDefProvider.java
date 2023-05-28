package org.dromara.northstar.gateway.sim;

import java.util.List;
import java.util.regex.Pattern;

import org.dromara.northstar.gateway.model.ContractDefinition;
import org.springframework.stereotype.Component;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@Component
public class SimContractDefProvider {

	public List<ContractDefinition> get(){
		return List.of(
			ContractDefinition.builder()
				.name("模拟合约A")
				.exchange(ExchangeEnum.SHFE)
				.productClass(ProductClassEnum.FUTURES)
				.symbolPattern(Pattern.compile("sim[0-9]{3,4}@.+"))
				.commissionFee(6)
				.build()
		);
	}
}
