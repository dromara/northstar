package org.dromara.northstar.strategy.api;

import org.dromara.northstar.common.constant.ClosingPolicy;
import org.dromara.northstar.common.constant.SignalOperation;

import xyz.redtorch.pb.CoreEnum.OffsetFlagEnum;
import xyz.redtorch.pb.CoreField.PositionField;

public interface ClosingStrategy {

	/**
	 * 解析操作
	 * @param opr
	 * @param accStore
	 * @return
	 */
	OffsetFlagEnum resolveOperation(SignalOperation opr, PositionField position);
	
	/**
	 * 获取平仓策略
	 * @return
	 */
	ClosingPolicy getClosingPolicy();
}
