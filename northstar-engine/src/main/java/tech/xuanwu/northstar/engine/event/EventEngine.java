package tech.xuanwu.northstar.engine.event;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;

import tech.xuanwu.northstar.common.event.NorthstarEvent;
import tech.xuanwu.northstar.common.event.NorthstarEventType;

/**
 * 事件引擎
 * 负责第三方网关的事件
 */
public interface EventEngine {

	void addHandler(NorthstarEventHandler handler);

	void removeHandler(NorthstarEventHandler handler);

	void emitEvent(NorthstarEventType event, Object obj);

	public static interface NorthstarEventHandler extends EventHandler<NorthstarEvent>{
	}
	
	public static class NorthstarEventFactory implements EventFactory<NorthstarEvent> {

		@Override
		public NorthstarEvent newInstance() {
			return new NorthstarEvent();
		}

	}

}
