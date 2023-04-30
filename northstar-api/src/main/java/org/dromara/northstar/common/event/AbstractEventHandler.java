package org.dromara.northstar.common.event;

public abstract class AbstractEventHandler implements GenericEventHandler{

	
	@Override
	public void onEvent(NorthstarEvent e) {
		if(canHandle(e.getEvent())) {
			doHandle(e);
		}
	}

	protected abstract void doHandle(NorthstarEvent e);
}
