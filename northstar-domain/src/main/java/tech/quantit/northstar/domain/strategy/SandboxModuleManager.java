package tech.quantit.northstar.domain.strategy;

import tech.quantit.northstar.common.event.NorthstarEventType;

public class SandboxModuleManager extends ModuleManager{

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		switch(eventType) {
		case ACCOUNT:
		case TRADE:
		case ORDER:
			return true;
		default:
			return false;
		}
	}

	
}
