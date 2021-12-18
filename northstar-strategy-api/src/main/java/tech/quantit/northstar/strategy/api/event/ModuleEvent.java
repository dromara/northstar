package tech.quantit.northstar.strategy.api.event;

import lombok.Getter;

/**
 * 
 * @author KevinHuangwl
 *
 */
@Getter
public class ModuleEvent<T> {

	private ModuleEventType eventType;
	
	private T data;
	
	public ModuleEvent(ModuleEventType eventType, T data) {
		this.eventType = eventType;
		this.data = data;
	}
}
