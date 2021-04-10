package tech.xuanwu.northstar.handler;

import com.google.common.eventbus.Subscribe;

import tech.xuanwu.northstar.common.event.NorthstarEvent;

public abstract class AbstractEventHandler implements InternalEventHandler{

	@Subscribe
	@Override
	public void onEvent(NorthstarEvent e) {
		if(canHandle(e.getEvent())) {
			doHandle(e);
		}
	}

	protected abstract void doHandle(NorthstarEvent e);
}
