package tech.xuanwu.northstar.strategy.common.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.TradeField;

@Document
@Data
public class TradeDescription {
	
	private String moduleName;

	private String unifiedSymbol;
	
	private String contractName;
	
	private double contractMultiplier;
	
	private DirectionEnum direction;
	
	private OffsetFlagEnum offsetFlag;
	
	private int volume;
	
	private double price;
	
	private String tradingDay;
	
	private long tradeTimestamp;
	
	public static TradeDescription convertFrom(String moduleName, TradeField trade) {
		TradeDescription des = new TradeDescription();
		des.unifiedSymbol = trade.getContract().getUnifiedSymbol();
		des.direction = trade.getDirection();
		des.contractName = trade.getContract().getName();
		des.offsetFlag = trade.getOffsetFlag();
		des.volume = trade.getVolume();
		des.price = trade.getPrice();
		des.tradingDay = trade.getTradingDay();
		des.tradeTimestamp = trade.getTradeTimestamp();
		des.contractMultiplier = trade.getContract().getMultiplier();
		des.moduleName = moduleName;
		return des;
	}
}
