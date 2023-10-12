package org.dromara.northstar.common.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListValue implements Value{
	
	private String name;

	private List<Value> value;
	
}
