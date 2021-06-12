package tech.xuanwu.northstar.strategy.common.model.meta;

import java.util.List;

import lombok.Data;

@Data
public class ComponentAndParamsPair {

	private ComponentMetaInfo componentMeta;
	private List<ComponentField> initParams;
}
