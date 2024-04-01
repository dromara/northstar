package org.dromara.northstar.ai;

public interface SamplingAware {

	/**
	 * 采样
	 * @return
	 */
	SampleData sample();
	
	/**
	 * 是否处于采样阶段
	 * @return
	 */
	boolean isSampling();
	
}
