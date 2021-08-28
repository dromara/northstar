package tech.xuanwu.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModulePositionEntity {

	private PositionDirectionEnum positionDir;
	
	private double openPrice;
	
	private double stopLossPrice;
	
	private String unifiedSymbol;
	
	private int volume;
	
	private int multiplier;
	
}
