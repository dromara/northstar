package org.dromara.northstar.common.model;

import org.dromara.northstar.common.constant.FieldType;

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
	
	private FieldType type;
	
	private Object value;
	
	private String unit;
	
	private String[] options;
	
	private String[] optionsVal;
	
	private String placeholder;
	
	private boolean required;
	
}
