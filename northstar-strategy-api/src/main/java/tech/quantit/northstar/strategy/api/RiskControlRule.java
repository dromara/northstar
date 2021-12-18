package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public interface RiskControlRule extends DynamicParamsAware, ModuleNamingAware {

	RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick);
}
