package tech.quantit.northstar.strategy.api.policy.risk;

import tech.quantit.northstar.strategy.api.RiskControlRule;

public abstract class AbstractRule implements RiskControlRule{
	
	protected String moduleName;

	@Override
	public void setModuleName(String name) {
		moduleName = name;
	}

	@Override
	public String getModuleName() {
		return moduleName;
	}

	
}
