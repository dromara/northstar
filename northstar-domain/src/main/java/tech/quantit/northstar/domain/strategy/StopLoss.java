package tech.quantit.northstar.domain.strategy;

import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.redtorch.pb.CoreEnum.PositionDirectionEnum;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 止损
 * @author KevinHuangwl
 *
 */
@NoArgsConstructor
@Data
public class StopLoss {

	private PositionDirectionEnum dir;
	
	private double stopPrice;
	
	public StopLoss(PositionDirectionEnum dir, double stopPrice) {
		this.dir = dir;
		this.stopPrice = stopPrice;
	}
	
	public boolean isTriggered(TickField tick) {
		return	stopPrice > 0 && 
				(dir == PositionDirectionEnum.PD_Long && tick.getLastPrice() <= stopPrice
				|| dir == PositionDirectionEnum.PD_Short && tick.getLastPrice() >= stopPrice);
	}
	
}
