package tech.quantit.northstar.gateway.playback.ticker;

import java.util.List;

import tech.quantit.northstar.common.constant.PlaybackPrecision;
import xyz.redtorch.pb.CoreField.BarField;
import xyz.redtorch.pb.CoreField.TickField;

/**
 * 简单的TICK仿真算法
 * 按照开高低收所经历的价位计算步长，然后进行插值
 * @author KevinHuangwl
 *
 */
public class SimpleTickSimulation implements TickSimulationAlgorithm{
	
	private PlaybackPrecision playbackPrecision;
	
	public SimpleTickSimulation(PlaybackPrecision playbackPrecision) {
		this.playbackPrecision = playbackPrecision;
	}

	@Override
	public List<TickField> generateFrom(BarField bar) {
		// TODO Auto-generated method stub
		return null;
	}

}
