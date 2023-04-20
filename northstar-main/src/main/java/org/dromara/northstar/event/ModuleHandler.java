package org.dromara.northstar.event;

import java.util.EnumSet;
import java.util.Set;

import org.dromara.northstar.common.event.AbstractEventHandler;
import org.dromara.northstar.common.event.GenericEventHandler;
import org.dromara.northstar.common.event.NorthstarEvent;
import org.dromara.northstar.common.event.NorthstarEventType;
import org.dromara.northstar.module.ModuleManager;



public class ModuleHandler extends AbstractEventHandler implements GenericEventHandler{

	private ModuleManager moduleMgr;
	
	private static final Set<NorthstarEventType> TARGET_TYPE = EnumSet.of(
			NorthstarEventType.TICK,
			NorthstarEventType.BAR,
			NorthstarEventType.TRADE,
			NorthstarEventType.ORDER
	); 
	
	public ModuleHandler(ModuleManager moduleMgr) {
		this.moduleMgr = moduleMgr;
	}
	
	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return TARGET_TYPE.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		moduleMgr.allModules().parallelStream().forEach(module -> module.onEvent(e));
	}

}
