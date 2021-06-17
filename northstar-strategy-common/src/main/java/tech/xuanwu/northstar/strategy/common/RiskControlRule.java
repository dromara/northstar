package tech.xuanwu.northstar.strategy.common;

/**
 * 风控策略用于限制信号的执行
 * @author KevinHuangwl
 *
 */
public interface RiskControlRule extends DynamicParamsAware{

	boolean canDeal(Signal signal);
}
