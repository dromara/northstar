package tech.xuanwu.northstar.strategy.common.model.entity;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleDataRef {

	/**
	 * BarField byte array list
	 */
	private Map<String, List<byte[]>> refBarDataMap;
	
}
