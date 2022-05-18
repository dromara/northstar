package tech.quantit.northstar.domain.module;

import tech.quantit.northstar.common.constant.ClosingPolicy;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.strategy.api.ClosingStrategy;
import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.PositionField;

public class PriorBeforeAndHedgeTodayClosingStrategy  implements ClosingStrategy{

	@Override
	public OffsetFlagEnum resolveOperation(SignalOperation opr, PositionField position) {
		if(opr.isOpen())	return OffsetFlagEnum.OF_Open;
		if(position.getYdPosition() - position.getYdFrozen() > 0)	return OffsetFlagEnum.OF_CloseYesterday;
		return OffsetFlagEnum.OF_Open;
	}
	
	@Override
	public ClosingPolicy getClosingPolicy() {
		return ClosingPolicy.PRIOR_BEFORE_HEGDE_TODAY;
	}

}
