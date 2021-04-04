package tech.xuanwu.northstar.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class NorthstarEvent {

	private NorthstarEventType event;
	private Object data;
}
