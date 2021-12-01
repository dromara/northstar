package tech.xuanwu.northstar.domain.strategy;

import tech.xuanwu.northstar.common.event.NorthstarEventType;

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
