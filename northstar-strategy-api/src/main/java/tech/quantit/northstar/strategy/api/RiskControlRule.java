package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.strategy.api.constant.RiskAuditResult;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Deprecated
public interface RiskControlRule extends DynamicParamsAware, ModuleNamingAware, MailSenderAware {

	RiskAuditResult checkRisk(SubmitOrderReqField orderReq, TickField tick);
}
