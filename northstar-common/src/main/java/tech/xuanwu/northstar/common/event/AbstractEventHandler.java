package tech.xuanwu.northstar.common.event;

import com.google.common.eventbus.Subscribe;

public abstract class AbstractEventHandler implements GenericEventHandler{

	@Subscribe
	@Override
	public void onEvent(NorthstarEvent e) {
		if(canHandle(e.getEvent())) {
			doHandle(e);
		}
	}

	protected abstract void doHandle(NorthstarEvent e);
}
