package tech.quantit.northstar.common.event;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;

/**
 * 事件引擎
 * 负责第三方网关的事件
 */
public interface FastEventEngine {

	void addHandler(NorthstarEventDispatcher handler);

	void removeHandler(NorthstarEventDispatcher handler);

	void emitEvent(NorthstarEventType event, Object obj);

	public static interface NorthstarEventDispatcher extends EventHandler<NorthstarEvent>{
	}
	
	public static class NorthstarEventFactory implements EventFactory<NorthstarEvent> {

		@Override
		public NorthstarEvent newInstance() {
			return new NorthstarEvent(null, null);
		}

	}

}
