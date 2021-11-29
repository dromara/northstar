package tech.xuanwu.northstar.strategy.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.xuanwu.northstar.common.utils.FieldUtils;
import xyz.redtorch.pb.CoreField.TradeField;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleTradeRecord {

	private String moduleName;

	private String contractName;
	
	private String operation;
	
	private String tradingDay;
	
	private long actionTime;
	
	private int volume;
	
	private double price;
	
	public static ModuleTradeRecord convertFrom(String moduleName, TradeField trade) {
		return ModuleTradeRecord.builder()
				.moduleName(moduleName)
				.contractName(trade.getContract().getSymbol())
				.operation(FieldUtils.chn(trade.getDirection()) + FieldUtils.chn(trade.getOffsetFlag()))
				.tradingDay(trade.getTradingDay())
				.actionTime(trade.getTradeTimestamp())
				.volume(trade.getVolume())
				.price(trade.getPrice())
				.build();
	}
}
