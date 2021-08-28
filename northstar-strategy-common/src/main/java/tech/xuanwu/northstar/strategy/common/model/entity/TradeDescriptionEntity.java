package tech.xuanwu.northstar.strategy.common.model.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.DirectionEnum;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
@Data
public class TradeDescriptionEntity {
	
	private String moduleName;

	private String symbol;
	
	private String gatewayId;
	
	private double contractMultiplier;
	
	private DirectionEnum direction;
	
	private OffsetFlagEnum offsetFlag;
	
	private int volume;
	
	private double price;
	
	private String tradingDay;
	
	private long tradeTimestamp;
	
}
