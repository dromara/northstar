package tech.quantit.northstar.gateway.playback.ticker;

import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;

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
	List<TickEntry> generateFrom(BarField bar);
}
