package tech.xuanwu.northstar.strategy.api;

import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public interface RiskControlRule extends DynamicParamsAware {

	int checkRisk(SubmitOrderReqField orderReq, TickField tick, ModuleStatus moduleStatus);
}
