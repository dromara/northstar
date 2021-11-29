package tech.xuanwu.northstar.strategy.api.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Data
public class ModulePositionInfo {

	@NotNull
	private String unifiedSymbol;
	@NotNull
	private PositionDirectionEnum positionDir;
	
	private String openTradingDay;
	
	private long openTime;
	
	private double multiplier;
	@NotNull
	@Min(value=1, message="开仓价格应该为正数")
	private double openPrice;
	@Min(value=0, message="止损价不能为负数")
	private double stopLossPrice;
	@NotNull
	@Min(value=1, message="手数应该为大于零的整数")
	private int volume;
}
