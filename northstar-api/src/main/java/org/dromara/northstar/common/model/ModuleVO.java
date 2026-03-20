package org.dromara.northstar.common.model;

import org.dromara.northstar.common.constant.ModuleState;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ModuleVO {

	private ModuleDescription description;
	
	private boolean enabled;
	
	private ModuleState state;
}
