package tech.xuanwu.northstar.strategy.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ComponentMetaInfo {

	private String name;
	
	private Class<?> clz;
}
