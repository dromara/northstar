package tech.xuanwu.northstar.strategy.common;

public interface DynamicParamsAware {

	/**
	 * 获取配置类
	 * @return
	 */
	DynamicParams<?> getDynamicParams();
}
