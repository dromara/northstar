package tech.quantit.northstar.strategy.api;

public interface ContextAware {

	void setContext(ModuleContext context);
	
	ModuleContext getContext();
}
