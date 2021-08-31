package tech.xuanwu.northstar.strategy.common.model;

import java.util.List;

import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.RiskController;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

public class GenericRiskController implements RiskController{
	
	private List<RiskControlRule> rules;
	
	protected SubmitOrderReqField curSubmitOrder;
	
	public GenericRiskController(List<RiskControlRule> rules) {
		this.rules = rules;
	}

	@Override
	public short onTick(TickField tick, ModuleStatus moduleStatus) {
		short result = RiskAuditResult.ACCEPTED;
		for(RiskControlRule rule : rules) {
			result |= rule.canDeal(tick, moduleStatus);
		}
		return result;
	}

	@Override
	public boolean testReject(TickField tick, ModuleStatus moduleStatus, SubmitOrderReqField orderReq) {
		for(RiskControlRule rule : rules) {
			rule.onSubmitOrder(orderReq);
		}
		return (onTick(tick, moduleStatus) & RiskAuditResult.REJECTED) > 0;
	}

}
