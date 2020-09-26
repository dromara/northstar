package tech.xuanwu.northstar.gateway;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;

/**
 * 事件引擎
 * 负责第三方网关的事件
 */
public interface FastEventEngine {

	void addHandler(FastEventHandler handler);

	void removeHandler(FastEventHandler handler);

	void emitEvent(EventType fastEventType, String event, Object obj);
	
	/**
	 * 事件代码
	 * @author kevinhuangwl
	 *
	 */
	public enum EventType {
		TICK,
		BAR,
		ACCOUNT,
		POSITION,
		TRADE,
		ORDER,
		NOTICE,
		CONTRACT,
		LIFECYCLE
	}

	public static class FastEvent {
		EventType eventType;
		private String event;

		private Object obj = null;

		public EventType getEventType() {
			return eventType;
		}

		public void setEventType(EventType eventType) {
			this.eventType = eventType;
		}

		public String getEvent() {
			return event;
		}

		public void setEvent(String event) {
			this.event = event;
		}

		public Object getObj() {
			return obj;
		}

		public void setObj(Object obj) {
			this.obj = obj;
		}
	}

	public static interface FastEventHandler extends EventHandler<FastEvent> {
	}
	
	public static class FastEventFactory implements EventFactory<FastEvent> {

		@Override
		public FastEvent newInstance() {
			return new FastEvent();
		}

	}

}
