package org.dromara.northstar.domain.module;

import java.util.Objects;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.SignalOperation;
import org.dromara.northstar.strategy.api.ClosingStrategy;

import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.PositionField;

public class PriorBeforeAndHedgeTodayClosingStrategy  implements ClosingStrategy{

	@Override
	public OffsetFlagEnum resolveOperation(SignalOperation opr, PositionField position) {
		if(Objects.nonNull(position) && position.getYdPosition() - position.getYdFrozen() > 0) {
			return OffsetFlagEnum.OF_Close;
		}
		return OffsetFlagEnum.OF_Open;
	}
	
	@Override
	public ClosingPolicy getClosingPolicy() {
		return ClosingPolicy.PRIOR_BEFORE_HEGDE_TODAY;
	}

}
