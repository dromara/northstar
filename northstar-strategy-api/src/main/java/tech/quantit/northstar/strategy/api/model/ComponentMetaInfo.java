package tech.quantit.northstar.strategy.api.model;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ComponentMetaInfo {

	private String name;
	
	@NotNull
	private String className;
}
