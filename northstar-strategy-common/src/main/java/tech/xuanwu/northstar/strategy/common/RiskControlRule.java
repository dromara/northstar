package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.constants.Signal;

public interface RiskControlRule {

	boolean canDeal(Signal signal);
}
