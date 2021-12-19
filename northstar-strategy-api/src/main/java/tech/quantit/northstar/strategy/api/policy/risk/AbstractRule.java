package tech.quantit.northstar.strategy.api.policy.risk;

import org.slf4j.Logger;

import tech.quantit.northstar.strategy.api.RiskControlRule;
import tech.quantit.northstar.strategy.api.log.NorthstarLoggerFactory;

public abstract class AbstractRule implements RiskControlRule{
	
	protected String moduleName;
	
	protected Logger log;

	@Override
	public void setModuleName(String name) {
		moduleName = name;
		log = NorthstarLoggerFactory.getLogger(name, getClass());
	}

	@Override
	public String getModuleName() {
		return moduleName;
	}

	
}
