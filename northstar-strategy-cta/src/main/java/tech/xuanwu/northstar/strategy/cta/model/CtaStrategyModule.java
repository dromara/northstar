package tech.xuanwu.northstar.strategy.cta.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import tech.xuanwu.northstar.strategy.common.DynamicParams;

@Document
@Data
public class CtaStrategyModule {

	private ComponentAndParamsPair signalPolicy;
	
	private List<ComponentAndParamsPair> riskControlPolicyList;
	
	private ComponentAndParamsPair dealer;
	
	private String accountGatewayId;
	
	@Id
	private String moduleName;
	
	private double allocatedAccountShare;
	
	private boolean enabled;
	
	@Data
	public static class ComponentAndParamsPair {
		
		private String componentName;
		private DynamicParams<?> initParams;
		
	}
	
}
