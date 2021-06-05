package tech.xuanwu.northstar.strategy.common.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document
@Data
public class CtaStrategyModule {

	private ComponentAndParamsPair signalPolicy;
	
	private List<ComponentAndParamsPair> riskControlRules;
	
	private ComponentAndParamsPair dealer;
	
	private String accountGatewayId;
	
	@Id
	private String moduleName;
	
	private double allocatedAccountShare;
	
	private boolean enabled;
	
}
