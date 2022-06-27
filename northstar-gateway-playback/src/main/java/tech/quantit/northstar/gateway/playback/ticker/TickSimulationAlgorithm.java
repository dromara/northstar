package tech.quantit.northstar.gateway.playback.ticker;

import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

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
	List<TickField> generateFrom(BarField bar);
}
