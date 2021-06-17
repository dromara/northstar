package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.StrategyModule;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 风控策略用于限制信号的执行
 * @author KevinHuangwl
 *
 */
public interface RiskControlRule extends DynamicParamsAware{

	boolean canDeal(TickField tick, StrategyModule module);
	
}
