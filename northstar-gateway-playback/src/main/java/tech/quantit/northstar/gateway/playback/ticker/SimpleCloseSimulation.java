package tech.quantit.northstar.gateway.playback.ticker;

import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;

/**
 * 收盘价生成策略
 * @author KevinHuangwl
 *
 */
public class SimpleCloseSimulation implements TickSimulationAlgorithm {
	
	@Override
	public List<TickEntry> generateFrom(BarField bar) {
		return List.of(TickEntry.of(bar.getClosePrice(), bar.getVolume(), bar.getOpenInterest(), bar.getActionTimestamp() - 30000));
	}

}
