package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.CtaSignal;

public interface RiskControlRule extends DynamicParamsAware{

	boolean canDeal(CtaSignal ctaSignal);
}
