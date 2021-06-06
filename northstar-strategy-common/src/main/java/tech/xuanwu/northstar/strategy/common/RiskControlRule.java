package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.Signal;

public interface RiskControlRule {

	boolean canDeal(Signal signal);
}
