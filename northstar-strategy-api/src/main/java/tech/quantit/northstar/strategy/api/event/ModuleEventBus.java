package tech.quantit.northstar.strategy.api.event;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;

public class ModuleEventBus extends AsyncEventBus{

	public ModuleEventBus(Executor executor) {
		super(executor);
	}
	
	public ModuleEventBus() {
		this(Executors.newSingleThreadExecutor());
	}

	@Override
	public void post(Object event) {
		super.post(event);
	}

	
}
