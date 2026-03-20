package org.dromara.northstar.common.model;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class StrategyMetaInfo {

	private String name;
	
	private String className;
	
	private Collection<ComponentField> settings;
}
