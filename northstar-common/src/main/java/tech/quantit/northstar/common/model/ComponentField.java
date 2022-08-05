package tech.quantit.northstar.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.quantit.northstar.common.constant.FieldType;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ComponentField {

	private String label;
	
	private String name;
	
	private int order;
	
	private FieldType type;
	
	private Object value;
	
	private String unit;
	
	private String[] options;
	
	private String[] optionsVal;
	
	private String placeholder;
	
}
