package tech.xuanwu.northstar.strategy.api.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ComponentAndParamsPair {

	private ComponentMetaInfo componentMeta;
	private List<ComponentField> initParams;
}
