package tech.quantit.northstar.strategy.api;

public interface ContextAware {

	void setContext(IModuleContext context);
	
	IModuleContext getContext();
}
