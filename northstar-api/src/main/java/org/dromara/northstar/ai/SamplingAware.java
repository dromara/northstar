package org.dromara.northstar.ai;

import java.util.function.Consumer;

public interface SamplingAware {

	/**
	 * 采样
	 * @return
	 */
	SampleData sample();
	
	/**
	 * 插入回调函数
	 */
	void setOnBarCallback(Consumer<SampleData> callback);
}
