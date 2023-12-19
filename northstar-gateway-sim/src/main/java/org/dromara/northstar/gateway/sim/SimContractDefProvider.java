package org.dromara.northstar.gateway.sim;

import java.util.List;
import java.util.regex.Pattern;

import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.DateTimeUtils;
import org.springframework.stereotype.Component;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@Component
public class SimContractDefProvider {
	
	private TimeSlot allDay = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(0, 0)).end(DateTimeUtils.fromCacheTime(0, 0)).build();

	public List<ContractDefinition> get(){
		return List.of(
			ContractDefinition.builder()
				.name("模拟合约")
				.exchange(ExchangeEnum.SHFE)
				.productClass(ProductClassEnum.FUTURES)
				.symbolPattern(Pattern.compile("sim[0-9]{3,4}@.+"))
				.commissionFee(6)
				.tradeTimeDef(TradeTimeDefinition.builder().timeSlots(List.of(allDay)).build())
				.build(),
			ContractDefinition.builder()
				.name("模拟合约")
				.exchange(ExchangeEnum.BINANCE)
				.productClass(ProductClassEnum.FUTURES)
				.symbolPattern(Pattern.compile("sim[0-9]{3,4}@.+"))
				.commissionRate(3.0E-4)
				.tradeTimeDef(TradeTimeDefinition.builder().timeSlots(List.of(allDay)).build())
				.build()
		);
	}
}
