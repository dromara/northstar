package tech.xuanwu.northstar.strategy.common;

import tech.xuanwu.northstar.strategy.common.model.meta.DynamicParams;

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
