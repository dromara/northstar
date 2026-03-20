package org.dromara.northstar.strategy;

import org.dromara.northstar.common.model.core.Bar;

/**
 * 复合行情BAR监听
 * @author KevinHuangwl
 *
 */
public interface MergedBarListener {

	void onMergedBar(Bar bar);
}
