package org.dromara.northstar.gateway.playback.ticker;

import java.util.List;

import org.dromara.northstar.common.model.core.Bar;

/**
 * TICK生成算法
 * @author KevinHuangwl
 *
 */
public interface TickSimulationAlgorithm {

	/**
	 * 根据BAR生成TICK
	 * @param bar
	 * @return
	 */
	List<TickEntry> generateFrom(Bar bar);
}
