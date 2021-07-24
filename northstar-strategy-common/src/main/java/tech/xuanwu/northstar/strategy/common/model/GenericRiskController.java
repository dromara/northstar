package tech.xuanwu.northstar.strategy.common.model;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.RiskController;
import tech.xuanwu.northstar.strategy.common.constants.RiskAuditResult;
import tech.xuanwu.northstar.strategy.common.model.state.ModuleStateMachine;
import xyz.redtorch.pb.CoreField.SubmitOrderReqField;
import xyz.redtorch.pb.CoreField.TickField;

@Slf4j
public class GenericRiskController implements RiskController{
	
	private List<RiskControlRule> rules;
	
	protected SubmitOrderReqField curSubmitOrder;
	
	public GenericRiskController(List<RiskControlRule> rules) {
		this.rules = rules;
	}

	@Override
	public short onTick(TickField tick, StrategyModule module) {
		short result = RiskAuditResult.ACCEPTED;
		for(RiskControlRule rule : rules) {
			result |= rule.canDeal(tick, module);
		}
		return result;
	}
	
	@Override
	public boolean testReject(SubmitOrderReqField orderReq) {
		// TODO Auto-generated method stub
		return false;
	}

}
