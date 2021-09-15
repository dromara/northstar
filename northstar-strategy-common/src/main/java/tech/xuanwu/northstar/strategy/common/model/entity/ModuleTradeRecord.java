package tech.xuanwu.northstar.strategy.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreField.TradeField;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleTradeRecord {

	private String moduleName;

	private String contractName;
	
	private DirectionEnum direction;
	
	private String tradingDay;
	
	private long actionTime;
	
	private int volume;
	
	private double price;
	
	public static ModuleTradeRecord convertFrom(String moduleName, TradeField trade) {
		return ModuleTradeRecord.builder()
				.moduleName(moduleName)
				.contractName(trade.getContract().getSymbol())
				.direction(trade.getDirection())
				.tradingDay(trade.getTradingDay())
				.actionTime(trade.getTradeTimestamp())
				.volume(trade.getVolume())
				.price(trade.getPrice())
				.build();
	}
}
