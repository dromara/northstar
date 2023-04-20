package org.dromara.northstar.strategy;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * BAR行情组件
 * @author KevinHuangwl
 *
 */
public interface BarDataAware {
	
	
	void onBar(BarField bar);
}
