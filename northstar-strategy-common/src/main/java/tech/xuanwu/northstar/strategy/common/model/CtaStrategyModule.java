package tech.xuanwu.northstar.strategy.common.model;

import java.util.List;

import lombok.Data;

@Data
public class CtaStrategyModule {

	private ComponentAndParamsPair signalPolicy;
	
	private List<ComponentAndParamsPair> riskControlRules;
	
	private ComponentAndParamsPair dealer;
	
	private String accountGatewayId;
	
	private String moduleName;
	
	private double allocatedAccountShare;
	
	private boolean enabled;
	
}
