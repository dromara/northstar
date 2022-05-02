package tech.quantit.northstar.domain.strategy;

import tech.quantit.northstar.common.event.NorthstarEventType;

@Deprecated
public class SandboxModuleManager extends ModuleManager{

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return switch(eventType) {
			case ACCOUNT, TRADE, ORDER, BAR, TICK -> true;
			default -> false;
		};
	}

	
}
