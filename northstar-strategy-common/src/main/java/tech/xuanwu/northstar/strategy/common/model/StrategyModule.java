package tech.xuanwu.northstar.strategy.common.model;

import java.util.Collections;
import java.util.List;

import tech.xuanwu.northstar.strategy.common.Dealer;
import tech.xuanwu.northstar.strategy.common.RiskControlRule;
import tech.xuanwu.northstar.strategy.common.SignalPolicy;

public class StrategyModule {
	
	private List<RiskControlRule> riskPolicies = Collections.EMPTY_LIST;
	
	private String accountGatewayId;
	
	private Dealer dealer;
	
	private SignalPolicy ocPolicy;

	public StrategyModule(String accountGatewayId) {
		this.accountGatewayId = accountGatewayId;
	}
	
	
	
	public void setRiskPolicies(List<RiskControlRule> policies) {
		this.riskPolicies = policies;
	}
}
