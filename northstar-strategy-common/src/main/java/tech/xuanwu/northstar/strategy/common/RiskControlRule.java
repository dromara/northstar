package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.ModuleAgent;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 风控策略用于限制信号的执行
 * @author KevinHuangwl
 *
 */
public interface RiskControlRule extends DynamicParamsAware {

	short canDeal(TickField tick, ModuleAgent agent);
	
	RiskControlRule onSubmitOrderReq(SubmitOrderReqField orderReq);
}
