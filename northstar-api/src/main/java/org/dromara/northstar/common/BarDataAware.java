package org.dromara.northstar.common;

import org.dromara.northstar.common.model.core.Bar;

/**
 * BAR行情组件
 * @author KevinHuangwl
 *
 */
public interface BarDataAware {
	
	
	void onBar(Bar bar);
}
