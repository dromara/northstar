package tech.xuanwu.northstar.strategy.common.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import lombok.Data;
import tech.xuanwu.northstar.strategy.common.constants.ModuleType;
import tech.xuanwu.northstar.strategy.common.model.meta.ComponentAndParamsPair;

@Document
@Data
public class ModuleInfo {

	private ComponentAndParamsPair signalPolicy;
	
	private List<ComponentAndParamsPair> riskControlRules;
	
	private ComponentAndParamsPair dealer;
	
	private String accountGatewayId;
	
	@Id
	private String moduleName;
	
	private double allocatedAccountShare;
	
	private boolean enabled;
	
	private ModuleType type;
	
}
