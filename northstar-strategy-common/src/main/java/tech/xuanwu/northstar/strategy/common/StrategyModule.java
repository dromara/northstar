package tech.xuanwu.northstar.strategy.common;

import java.util.Collections;
import java.util.List;

public class StrategyModule {
	
	private List<RiskControlPolicy> riskPolicies = Collections.EMPTY_LIST;
	
	private String accountGatewayId;
	
	private Dealer dealer;
	
	private SignalPolicy ocPolicy;

	public StrategyModule(String accountGatewayId) {
		this.accountGatewayId = accountGatewayId;
	}
	
	
	
	public void setRiskPolicies(List<RiskControlPolicy> policies) {
		this.riskPolicies = policies;
	}
}
