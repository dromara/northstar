package tech.xuanwu.northstar.strategy.common.model.meta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ComponentField {

	private String label;
	
	private String name;
	
	private int order;
	
	private String type;
	
	private Object value;
	
	private String unit;
	
	private String[] options;
	
}
