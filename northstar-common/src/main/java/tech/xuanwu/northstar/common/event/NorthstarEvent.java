package tech.xuanwu.northstar.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class NorthstarEvent {

	private NorthstarEventType event;
	private Object obj = null;
}
