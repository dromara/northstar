package tech.xuanwu.northstar.engin.event;

import tech.xuanwu.northstar.common.event.NorthstarEvent;

/**
 * 事件引擎
 * 负责第三方网关的事件
 */
public interface EventEngine {

	void addHandler(EventHandler handler);

	void removeHandler(EventHandler handler);

	void emitEvent(NorthstarEvent event, Object obj);

	public static class Event {
		private NorthstarEvent event;
		private Object obj = null;

		public NorthstarEvent getEvent() {
			return event;
		}

		public void setEvent(NorthstarEvent event) {
			this.event = event;
		}

		public Object getObj() {
			return obj;
		}

		public void setObj(Object obj) {
			this.obj = obj;
		}
	}

	public static interface EventHandler {
	}
	
//	public static class FastEventFactory implements EventFactory<FastEvent> {
//
//		@Override
//		public FastEvent newInstance() {
//			return new FastEvent();
//		}
//
//	}

}
