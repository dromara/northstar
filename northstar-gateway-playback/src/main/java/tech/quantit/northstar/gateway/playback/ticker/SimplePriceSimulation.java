package tech.quantit.northstar.gateway.playback.ticker;

import java.util.List;

import xyz.redtorch.pb.CoreField.BarField;

public class SimplePriceSimulation implements TickSimulationAlgorithm {

	@Override
	public List<TickEntry> generateFrom(BarField bar) {
		return List.of(
					TickEntry.of(bar.getOpenPrice(), bar.getVolume() / 4, bar.getOpenInterest(), bar.getActionTimestamp() - 50000),
					TickEntry.of(bar.getHighPrice(), bar.getVolume() / 4, bar.getOpenInterest(), bar.getActionTimestamp() - 40000),
					TickEntry.of(bar.getLowPrice(), bar.getVolume() / 4, bar.getOpenInterest(), bar.getActionTimestamp() - 30000),
					TickEntry.of(bar.getClosePrice(), bar.getVolume() / 4, bar.getOpenInterest(), bar.getActionTimestamp() - 20000)
				);
	}

}
