package tech.xuanwu.northstar.strategy.api;

import tech.xuanwu.northstar.strategy.api.constant.RiskAuditResult;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public interface RiskControlRule extends DynamicParamsAware {

	RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick, ModuleStatus moduleStatus);
}
