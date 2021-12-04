package tech.quantit.northstar.strategy.api;

import tech.quantit.northstar.strategy.api.model.DynamicParams;

public interface DynamicParamsAware {

	/**
	 * 获取配置类
	 * @return
	 */
	DynamicParams getDynamicParams();
	
	/**
	 * 通过配置类初始化
	 * @param params
	 */
	void initWithParams(DynamicParams params);
}
