package tech.xuanwu.northstar.engine.event;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.xuanwu.northstar.common.event.NorthstarEventType;

/**
 * 事件引擎
 * 负责第三方网关的事件
 */
public interface EventEngine {

	void addHandler(NorthstarEventHandler handler);

	void removeHandler(NorthstarEventHandler handler);

	void emitEvent(NorthstarEventType event, Object obj);

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class Event {
		private NorthstarEventType event;
		private Object obj = null;

	}

	public static interface NorthstarEventHandler extends EventHandler<Event>{
	}
	
	public static class NorthstarEventFactory implements EventFactory<Event> {

		@Override
		public Event newInstance() {
			return new Event();
		}

	}

}
