package tech.xuanwu.northstar.strategy.common.event;

import lombok.Builder;
import lombok.Data;

/**
 * 
 * @author KevinHuangwl
 *
 */
@Data
@Builder
public class ModuleEvent {

	private ModuleEventType eventType;
	
	private Object data;
}
