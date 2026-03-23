package org.dromara.northstar.gateway.sim;

import java.util.List;
import java.util.regex.Pattern;

import org.dromara.northstar.common.model.core.ContractDefinition;
import org.dromara.northstar.common.model.core.TimeSlot;
import org.dromara.northstar.common.model.core.TradeTimeDefinition;
import org.dromara.northstar.common.utils.DateTimeUtils;
import org.dromara.northstar.gateway.IContractDefinitionProvider;
import org.dromara.northstar.gateway.mktdata.EmptyDataSource;
import org.springframework.stereotype.Component;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;
import xyz.redtorch.pb.CoreEnum.ProductClassEnum;

@Component
public class SimContractDefProvider implements IContractDefinitionProvider {
	
	private TimeSlot allDay = TimeSlot.builder().start(DateTimeUtils.fromCacheTime(0, 0)).end(DateTimeUtils.fromCacheTime(0, 0)).build();
	
	private EmptyDataSource dummyDS = new EmptyDataSource();

	@Override
	public List<ContractDefinition> get(){
		return List.of(
			ContractDefinition.builder()
				.name("模拟合约")
				.exchange(ExchangeEnum.SHFE)
				.productClass(ProductClassEnum.FUTURES)
				.symbolPattern(Pattern.compile("sim[0-9]{3,4}@.+"))
				.commissionFee(6D)
				.marginRatio(0.1)
				.tradeTimeDef(TradeTimeDefinition.builder().timeSlots(List.of(allDay)).build())
				.dataSource(dummyDS)
				.build(),
			ContractDefinition.builder()
				.name("模拟合约")
				.exchange(ExchangeEnum.BINANCE)
				.productClass(ProductClassEnum.FUTURES)
				.symbolPattern(Pattern.compile("sim[0-9]{3,4}@.+"))
				.commissionRate(3.0E-4)
				.marginRatio(0.1)
				.tradeTimeDef(TradeTimeDefinition.builder().timeSlots(List.of(allDay)).build())
				.dataSource(dummyDS)
				.build()
		);
	}
}
