package tech.xuanwu.northstar.main.manager;

import tech.xuanwu.northstar.common.event.NorthstarEventType;
import tech.xuanwu.northstar.main.persistence.ModuleRepository;

public class SandboxModuleManager extends ModuleManager{

	public SandboxModuleManager(ModuleRepository moduleRepo) {
		super(moduleRepo);
	}

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
